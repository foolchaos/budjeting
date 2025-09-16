package com.example.budget.ui;

import com.example.budget.domain.*;
import com.example.budget.repo.*;
import com.example.budget.service.BdzService;
import com.example.budget.service.BoService;
import com.example.budget.service.ContractService;
import com.example.budget.service.ZgdService;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.spring.annotation.UIScope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Supplier;

@Component
@UIScope
public class ReferencesView extends SplitLayout {

    private final BdzService bdzService;
    private final BoService boService;
    private final ZgdService zgdService;
    private final CfoRepository cfoRepository;
    private final MvzRepository mvzRepository;
    private final ContractService contractService;

    private final ListBox<String> leftMenu = new ListBox<>();
    private final Div rightPanel = new Div();

    public ReferencesView(BdzService bdzService, BoService boService, ZgdService zgdService,
                          CfoRepository cfoRepository, MvzRepository mvzRepository,
                          ContractService contractService) {
        this.bdzService = bdzService;
        this.boService = boService;
        this.zgdService = zgdService;
        this.cfoRepository = cfoRepository;
        this.mvzRepository = mvzRepository;
        this.contractService = contractService;

        setSizeFull();
        leftMenu.setItems("БДЗ", "БО", "ЗГД", "ЦФО", "МВЗ", "Договор");
        leftMenu.setValue("БДЗ");
        leftMenu.addValueChangeListener(e -> renderRight(e.getValue()));

        rightPanel.setSizeFull();
        setOrientation(SplitLayout.Orientation.HORIZONTAL);
        setSplitterPosition(20);
        addToPrimary(leftMenu);
        addToSecondary(rightPanel);

        renderRight("БДЗ");
    }

    private void renderRight(String name) {
        rightPanel.removeAll();
        switch (name) {
            case "БДЗ" -> rightPanel.add(bdzTree());
            case "БО" -> rightPanel.add(boGrid());
            case "ЗГД" -> rightPanel.add(zgdGrid());
            case "ЦФО" -> rightPanel.add(cfoGrid());
            case "МВЗ" -> rightPanel.add(mvzGrid());
            case "Договор" -> rightPanel.add(contractGrid());
        }
    }

    // ==== BDZ Tree ====
    private VerticalLayout bdzTree() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        Button create = new Button("Создать");
        Button delete = new Button("Удалить выбранные");
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);

        TreeGrid<Bdz> tree = new TreeGrid<>();
        tree.setSelectionMode(Grid.SelectionMode.MULTI);
        tree.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        tree.setHeightFull();

        TextField codeFilter = new TextField();
        codeFilter.setValueChangeMode(ValueChangeMode.EAGER);
        codeFilter.setClearButtonVisible(true);

        TextField nameFilter = new TextField();
        nameFilter.setValueChangeMode(ValueChangeMode.EAGER);
        nameFilter.setClearButtonVisible(true);

        tree.addHierarchyColumn(Bdz::getCode)
                .setHeader(columnHeaderWithFilter("Код", codeFilter));
        tree.addColumn(Bdz::getName)
                .setHeader(columnHeaderWithFilter("Наименование", nameFilter));

        Select<Integer> pageSizeSelect = new Select<>();
        pageSizeSelect.setLabel("Строк на странице");
        pageSizeSelect.setItems(5, 10, 25, 50);
        pageSizeSelect.setValue(10);

        Button prev = new Button("Назад");
        Button next = new Button("Вперёд");
        Span pageInfo = new Span();

        List<Bdz> roots = new ArrayList<>();
        final int[] currentPage = {0};
        Map<Long, List<Bdz>> filteredChildren = new HashMap<>();
        AtomicBoolean filteredMode = new AtomicBoolean(false);

        Runnable updatePage = () -> {
            int pageSize = pageSizeSelect.getValue();
            int total = roots.size();

            if (total == 0) {
                tree.setItems(Collections.emptyList(), b -> Collections.emptyList());
                pageInfo.setText("0 из 0");
                prev.setEnabled(false);
                next.setEnabled(false);
                return;
            }

            int pageCount = (int) Math.ceil((double) total / pageSize);
            if (currentPage[0] >= pageCount) {
                currentPage[0] = Math.max(pageCount - 1, 0);
            }

            int from = currentPage[0] * pageSize;
            int to = Math.min(from + pageSize, total);
            List<Bdz> pageItems = new ArrayList<>(roots.subList(from, to));
            if (filteredMode.get()) {
                tree.setItems(pageItems, b -> filteredChildren.getOrDefault(b.getId(), List.of()));
                tree.expandRecursively(pageItems, Integer.MAX_VALUE);
            } else {
                tree.setItems(pageItems, b -> bdzService.findChildren(b.getId()));
            }
            pageInfo.setText(String.format("%d–%d из %d", from + 1, to, total));

            prev.setEnabled(currentPage[0] > 0);
            next.setEnabled(currentPage[0] < pageCount - 1);
        };

        Runnable reload = () -> {
            roots.clear();
            filteredChildren.clear();
            String codeValue = normalizeFilterValue(codeFilter.getValue());
            String nameValue = normalizeFilterValue(nameFilter.getValue());
            boolean hasFilter = (codeValue != null) || (nameValue != null);

            if (!hasFilter) {
                filteredMode.set(false);
                roots.addAll(bdzService.findRoots());
            } else {
                filteredMode.set(true);
                List<Bdz> allItems = bdzService.findAll();
                Set<Long> includedIds = new LinkedHashSet<>();

                for (Bdz item : allItems) {
                    if (matchesBdzFilters(item, codeValue, nameValue)) {
                        Bdz current = item;
                        while (current != null && current.getId() != null) {
                            includedIds.add(current.getId());
                            current = current.getParent();
                        }
                    }
                }

                Set<Long> seen = new LinkedHashSet<>();
                for (Bdz item : allItems) {
                    Long id = item.getId();
                    if (id == null || !includedIds.contains(id) || !seen.add(id)) {
                        continue;
                    }
                    Bdz parent = item.getParent();
                    if (parent == null || parent.getId() == null || !includedIds.contains(parent.getId())) {
                        roots.add(item);
                    } else {
                        filteredChildren.computeIfAbsent(parent.getId(), k -> new ArrayList<>()).add(item);
                    }
                }
            }
            currentPage[0] = 0;
            updatePage.run();
        };

        pageSizeSelect.addValueChangeListener(e -> {
            currentPage[0] = 0;
            updatePage.run();
        });

        prev.addClickListener(e -> {
            if (currentPage[0] > 0) {
                currentPage[0]--;
                updatePage.run();
            }
        });

        next.addClickListener(e -> {
            int pageSize = pageSizeSelect.getValue();
            if ((currentPage[0] + 1) * pageSize < roots.size()) {
                currentPage[0]++;
                updatePage.run();
            }
        });

        // open details on item click
        tree.addItemClickListener(ev -> openBdzCard(ev.getItem(), reload));

        create.addClickListener(e -> openBdzCard(new Bdz(), reload));
        delete.addClickListener(e -> {
            tree.getSelectedItems().forEach(item -> {
                if (item.getId() != null) bdzService.deleteById(item.getId());
            });
            reload.run();
        });

        codeFilter.addValueChangeListener(e -> reload.run());
        nameFilter.addValueChangeListener(e -> reload.run());

        HorizontalLayout pagination = new HorizontalLayout(prev, next, pageInfo, pageSizeSelect);
        pagination.setAlignItems(Alignment.CENTER);
        pagination.setWidthFull();
        pageInfo.getStyle().set("margin-left", "auto");
        pageInfo.getStyle().set("margin-right", "var(--lumo-space-m)");

        layout.add(new HorizontalLayout(create, delete), tree, pagination);
        layout.setFlexGrow(1, tree);
        reload.run();
        return layout;
    }

    private boolean matchesBdzFilters(Bdz item, String normalizedCodeFilter, String normalizedNameFilter) {
        boolean codeMatches = containsNormalized(item.getCode(), normalizedCodeFilter);
        boolean nameMatches = containsNormalized(item.getName(), normalizedNameFilter);
        return codeMatches && nameMatches;
    }

    private boolean matchesBoFilters(Bo item, String normalizedCodeFilter, String normalizedNameFilter, String normalizedBdzFilter) {
        if (!containsNormalized(item.getCode(), normalizedCodeFilter)) {
            return false;
        }
        if (!containsNormalized(item.getName(), normalizedNameFilter)) {
            return false;
        }

        if (normalizedBdzFilter == null) {
            return true;
        }

        if (item.getBdz() == null) {
            return false;
        }

        return containsNormalized(item.getBdz().getCode(), normalizedBdzFilter)
                || containsNormalized(item.getBdz().getName(), normalizedBdzFilter);
    }

    private boolean matchesZgdFilters(Zgd item,
                                      String normalizedFullNameFilter,
                                      String normalizedDepartmentFilter,
                                      String normalizedBdzFilter) {
        if (!containsNormalized(item.getFullName(), normalizedFullNameFilter)) {
            return false;
        }
        if (!containsNormalized(item.getDepartment(), normalizedDepartmentFilter)) {
            return false;
        }

        if (normalizedBdzFilter == null) {
            return true;
        }

        if (item.getBdz() == null) {
            return false;
        }

        return containsNormalized(item.getBdz().getCode(), normalizedBdzFilter)
                || containsNormalized(item.getBdz().getName(), normalizedBdzFilter);
    }

    private boolean matchesCfoFilters(Cfo item, String normalizedCodeFilter, String normalizedNameFilter) {
        return containsNormalized(item.getCode(), normalizedCodeFilter)
                && containsNormalized(item.getName(), normalizedNameFilter);
    }

    private Div columnHeaderWithFilter(String title, com.vaadin.flow.component.Component filter) {
        Div wrapper = new Div();
        wrapper.getStyle().set("display", "flex");
        wrapper.getStyle().set("flex-direction", "column");
        wrapper.getStyle().set("gap", "var(--lumo-space-xs)");
        wrapper.getStyle().set("width", "100%");

        Span caption = new Span(title);
        caption.getStyle().set("font-size", "var(--lumo-font-size-s)");
        caption.getStyle().set("font-weight", "600");

        if (filter instanceof HasSize sized) {
            sized.setWidthFull();
        }
        filter.getElement().getStyle().set("minWidth", "0");
        wrapper.add(caption, filter);
        return wrapper;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeFilterValue(String raw) {
        String trimmed = trimToNull(raw);
        return trimmed != null ? trimmed.toLowerCase(Locale.ROOT) : null;
    }

    private boolean containsNormalized(String value, String normalizedFilter) {
        if (normalizedFilter == null) {
            return true;
        }
        if (value == null) {
            return false;
        }
        return value.toLowerCase(Locale.ROOT).contains(normalizedFilter);
    }

    private void openBdzCard(Bdz entity, Runnable refresh) {
        Bdz bean = entity.getId() != null ? bdzService.findById(entity.getId()) : entity;

        Dialog dlg = new Dialog("Статья БДЗ");
        dlg.setWidth("500px");
        Binder<Bdz> binder = new Binder<>(Bdz.class);

        TextField code = new TextField("Код");
        TextField name = new TextField("Наименование");
        ComboBox<Bdz> parent = new ComboBox<>("Родитель");
        java.util.List<Bdz> options = bdzService.findAll().stream()
                .filter(b -> !Objects.equals(b.getId(), bean.getId()))
                .toList();
        parent.setItems(options);
        parent.setItemLabelGenerator(Bdz::getName);
        if (bean.getParent() != null) {
            Long pid = bean.getParent().getId();
            bean.setParent(options.stream()
                    .filter(b -> Objects.equals(b.getId(), pid))
                    .findFirst().orElse(null));
        }

        binder.bind(code, Bdz::getCode, Bdz::setCode);
        binder.bind(name, Bdz::getName, Bdz::setName);
        binder.bind(parent, Bdz::getParent, Bdz::setParent);
        binder.setBean(bean);

        Button save = new Button("Сохранить", e -> {
            bdzService.save(binder.getBean());
            refresh.run();
            dlg.close();
        });
        Button delete = new Button("Удалить", e -> {
            if (bean.getId() != null) {
                bdzService.deleteById(bean.getId());
                refresh.run();
            }
            dlg.close();
        });
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        Button close = new Button("Закрыть", e -> dlg.close());

        FormLayout form = new FormLayout(code, name, parent);
        dlg.add(form, new HorizontalLayout(save, delete, close));
        dlg.open();
    }

    // ==== Generic helpers for other refs ====
    private <T> VerticalLayout genericGrid(Class<T> type, Grid<T> grid,
                                           Supplier<List<T>> loader,
                                           java.util.function.Function<T, T> saver,
                                           java.util.function.Consumer<T> deleter,
                                           BiFunction<T, Runnable, Dialog> editorFactory) {
        return genericGrid(type, grid, loader, saver, deleter, editorFactory, null);
    }

    private <T> VerticalLayout genericGrid(Class<T> type, Grid<T> grid,
                                           Supplier<List<T>> loader,
                                           java.util.function.Function<T, T> saver,
                                           java.util.function.Consumer<T> deleter,
                                           BiFunction<T, Runnable, Dialog> editorFactory,
                                           java.util.function.Consumer<Runnable> refreshObserver) {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        Button create = new Button("Создать");
        Button delete = new Button("Удалить выбранные");
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setHeightFull();

        Select<Integer> pageSizeSelect = new Select<>();
        pageSizeSelect.setLabel("Строк на странице");
        pageSizeSelect.setItems(5, 10, 25, 50);
        pageSizeSelect.setValue(10);

        Button prev = new Button("Назад");
        Button next = new Button("Вперёд");
        Span pageInfo = new Span();

        List<T> items = new ArrayList<>();
        final int[] currentPage = {0};

        Runnable updatePage = () -> {
            int pageSize = pageSizeSelect.getValue();
            int total = items.size();

            if (total == 0) {
                grid.setItems(List.of());
                pageInfo.setText("0 из 0");
                prev.setEnabled(false);
                next.setEnabled(false);
                return;
            }

            int pageCount = (int) Math.ceil((double) total / pageSize);
            if (currentPage[0] >= pageCount) {
                currentPage[0] = Math.max(pageCount - 1, 0);
            }

            int from = currentPage[0] * pageSize;
            int to = Math.min(from + pageSize, total);
            grid.setItems(items.subList(from, to));
            pageInfo.setText(String.format("%d–%d из %d", from + 1, to, total));

            prev.setEnabled(currentPage[0] > 0);
            next.setEnabled(currentPage[0] < pageCount - 1);
        };

        Runnable refresh = () -> {
            items.clear();
            items.addAll(loader.get());
            currentPage[0] = 0;
            grid.deselectAll();
            updatePage.run();
        };

        if (refreshObserver != null) {
            refreshObserver.accept(refresh);
        }

        pageSizeSelect.addValueChangeListener(e -> {
            currentPage[0] = 0;
            updatePage.run();
        });

        prev.addClickListener(e -> {
            if (currentPage[0] > 0) {
                currentPage[0]--;
                updatePage.run();
            }
        });

        next.addClickListener(e -> {
            int pageSize = pageSizeSelect.getValue();
            if ((currentPage[0] + 1) * pageSize < items.size()) {
                currentPage[0]++;
                updatePage.run();
            }
        });

        grid.addItemClickListener(e -> editorFactory.apply(e.getItem(), refresh).open());
        create.addClickListener(e -> editorFactory.apply(null, refresh).open());
        delete.addClickListener(e -> {
            grid.getSelectedItems().forEach(deleter);
            refresh.run();
        });

        HorizontalLayout actions = new HorizontalLayout(create, delete);
        HorizontalLayout pagination = new HorizontalLayout(prev, next, pageInfo, pageSizeSelect);
        pagination.setAlignItems(Alignment.CENTER);
        pagination.setWidthFull();
        pageInfo.getStyle().set("margin-left", "auto");
        pageInfo.getStyle().set("margin-right", "var(--lumo-space-m)");

        layout.add(actions, grid, pagination);
        layout.setFlexGrow(1, grid);
        refresh.run();
        return layout;
    }

    private VerticalLayout boGrid() {
        Grid<Bo> grid = new Grid<>(Bo.class, false);

        TextField codeFilter = new TextField();
        codeFilter.setValueChangeMode(ValueChangeMode.EAGER);
        codeFilter.setClearButtonVisible(true);

        TextField nameFilter = new TextField();
        nameFilter.setValueChangeMode(ValueChangeMode.EAGER);
        nameFilter.setClearButtonVisible(true);

        TextField bdzFilter = new TextField();
        bdzFilter.setValueChangeMode(ValueChangeMode.EAGER);
        bdzFilter.setClearButtonVisible(true);

        grid.addColumn(Bo::getCode)
                .setHeader(columnHeaderWithFilter("Код", codeFilter));
        grid.addColumn(Bo::getName)
                .setHeader(columnHeaderWithFilter("Наименование", nameFilter));
        grid.addColumn(item -> {
            if (item.getBdz() == null) {
                return "—";
            }
            Bdz bdz = item.getBdz();
            String code = bdz.getCode() != null ? bdz.getCode() : "";
            String name = bdz.getName() != null ? bdz.getName() : "";
            return (code + " " + name).trim();
        }).setHeader(columnHeaderWithFilter("БДЗ", bdzFilter));

        Supplier<List<Bo>> loader = () -> {
            String codeValue = normalizeFilterValue(codeFilter.getValue());
            String nameValue = normalizeFilterValue(nameFilter.getValue());
            String bdzValue = normalizeFilterValue(bdzFilter.getValue());
            return boService.findAll().stream()
                    .filter(item -> matchesBoFilters(item, codeValue, nameValue, bdzValue))
                    .toList();
        };

        Runnable[] refreshHolder = new Runnable[1];

        VerticalLayout layout = genericGrid(Bo.class, grid,
                loader,
                boService::save,
                boService::delete,
                (selected, refresh) -> {
                    Bo bean = selected != null ? selected : new Bo();
                    Dialog d = new Dialog("Статья БО");
                    Binder<Bo> binder = new Binder<>(Bo.class);
                    TextField code = new TextField("Код");
                    TextField name = new TextField("Наименование");
                    ComboBox<Bdz> bdz = new ComboBox<>("Статья БДЗ");
                    bdz.setItems(bdzService.findAll());
                    bdz.setItemLabelGenerator(b -> {
                        String bdzCode = b.getCode() != null ? b.getCode() : "";
                        String bdzName = b.getName() != null ? b.getName() : "";
                        return (bdzCode + " " + bdzName).trim();
                    });
                    bdz.setClearButtonVisible(true);
                    binder.bind(code, Bo::getCode, Bo::setCode);
                    binder.bind(name, Bo::getName, Bo::setName);
                    binder.bind(bdz, Bo::getBdz, Bo::setBdz);
                    binder.setBean(bean);
                    Button save = new Button("Сохранить", e -> {
                        boService.save(binder.getBean());
                        Runnable targetRefresh = refreshHolder[0] != null ? refreshHolder[0] : refresh;
                        targetRefresh.run();
                        d.close();
                    });
                    Button del = new Button("Удалить", e -> {
                        if (bean.getId()!=null) {
                            boService.delete(bean);
                        }
                        Runnable targetRefresh = refreshHolder[0] != null ? refreshHolder[0] : refresh;
                        targetRefresh.run();
                        d.close();
                    });
                    del.addThemeVariants(ButtonVariant.LUMO_ERROR);
                    Button close = new Button("Закрыть", e -> d.close());
                    d.add(new FormLayout(code, name, bdz), new HorizontalLayout(save, del, close));
                    return d;
                },
                refresh -> refreshHolder[0] = refresh);

        codeFilter.addValueChangeListener(e -> {
            if (refreshHolder[0] != null) {
                refreshHolder[0].run();
            }
        });
        nameFilter.addValueChangeListener(e -> {
            if (refreshHolder[0] != null) {
                refreshHolder[0].run();
            }
        });
        bdzFilter.addValueChangeListener(e -> {
            if (refreshHolder[0] != null) {
                refreshHolder[0].run();
            }
        });

        return layout;
    }

    private VerticalLayout zgdGrid() {
        Grid<Zgd> grid = new Grid<>(Zgd.class, false);

        TextField fullNameFilter = new TextField();
        fullNameFilter.setValueChangeMode(ValueChangeMode.EAGER);
        fullNameFilter.setClearButtonVisible(true);

        TextField departmentFilter = new TextField();
        departmentFilter.setValueChangeMode(ValueChangeMode.EAGER);
        departmentFilter.setClearButtonVisible(true);

        TextField bdzFilter = new TextField();
        bdzFilter.setValueChangeMode(ValueChangeMode.EAGER);
        bdzFilter.setClearButtonVisible(true);

        grid.addColumn(Zgd::getFullName)
                .setHeader(columnHeaderWithFilter("ФИО", fullNameFilter));
        grid.addColumn(Zgd::getDepartment)
                .setHeader(columnHeaderWithFilter("Департамент", departmentFilter));
        grid.addColumn(item -> {
            if (item.getBdz() == null) {
                return "—";
            }
            Bdz bdz = item.getBdz();
            String code = bdz.getCode() != null ? bdz.getCode() : "";
            String name = bdz.getName() != null ? bdz.getName() : "";
            return (code + " " + name).trim();
        }).setHeader(columnHeaderWithFilter("БДЗ", bdzFilter));

        Supplier<List<Zgd>> loader = () -> {
            String fullNameValue = normalizeFilterValue(fullNameFilter.getValue());
            String departmentValue = normalizeFilterValue(departmentFilter.getValue());
            String bdzValue = normalizeFilterValue(bdzFilter.getValue());
            return zgdService.findAll().stream()
                    .filter(item -> matchesZgdFilters(item, fullNameValue, departmentValue, bdzValue))
                    .toList();
        };

        Runnable[] refreshHolder = new Runnable[1];

        VerticalLayout layout = genericGrid(Zgd.class, grid,
                loader,
                zgdService::save,
                zgdService::delete,
                (selected, refresh) -> {
                    Zgd bean = selected != null ? selected : new Zgd();
                    Dialog d = new Dialog("Курирующий ЗГД");
                    Binder<Zgd> binder = new Binder<>(Zgd.class);
                    TextField fio = new TextField("ФИО");
                    TextField dep = new TextField("Департамент");
                    ComboBox<Bdz> bdz = new ComboBox<>("Статья БДЗ");
                    bdz.setItems(bdzService.findAll());
                    bdz.setItemLabelGenerator(item -> {
                        String code = item.getCode() != null ? item.getCode() : "";
                        String name = item.getName() != null ? item.getName() : "";
                        return (code + " " + name).trim();
                    });
                    bdz.setClearButtonVisible(true);

                    binder.bind(fio, Zgd::getFullName, Zgd::setFullName);
                    binder.bind(dep, Zgd::getDepartment, Zgd::setDepartment);
                    binder.bind(bdz, Zgd::getBdz, Zgd::setBdz);
                    binder.setBean(bean);

                    Button save = new Button("Сохранить", e -> {
                        zgdService.save(binder.getBean());
                        Runnable targetRefresh = refreshHolder[0] != null ? refreshHolder[0] : refresh;
                        targetRefresh.run();
                        d.close();
                    });
                    Button del = new Button("Удалить", e -> {
                        if (bean.getId() != null) {
                            zgdService.delete(bean);
                        }
                        Runnable targetRefresh = refreshHolder[0] != null ? refreshHolder[0] : refresh;
                        targetRefresh.run();
                        d.close();
                    });
                    del.addThemeVariants(ButtonVariant.LUMO_ERROR);
                    Button close = new Button("Закрыть", e -> d.close());
                    d.add(new FormLayout(fio, dep, bdz), new HorizontalLayout(save, del, close));
                    return d;
                },
                refresh -> refreshHolder[0] = refresh);

        fullNameFilter.addValueChangeListener(e -> {
            if (refreshHolder[0] != null) {
                refreshHolder[0].run();
            }
        });
        departmentFilter.addValueChangeListener(e -> {
            if (refreshHolder[0] != null) {
                refreshHolder[0].run();
            }
        });
        bdzFilter.addValueChangeListener(e -> {
            if (refreshHolder[0] != null) {
                refreshHolder[0].run();
            }
        });

        return layout;
    }

    private VerticalLayout cfoGrid() {
        Grid<Cfo> grid = new Grid<>(Cfo.class, false);

        TextField codeFilter = new TextField();
        codeFilter.setValueChangeMode(ValueChangeMode.EAGER);
        codeFilter.setClearButtonVisible(true);

        TextField nameFilter = new TextField();
        nameFilter.setValueChangeMode(ValueChangeMode.EAGER);
        nameFilter.setClearButtonVisible(true);

        grid.addColumn(Cfo::getCode)
                .setHeader(columnHeaderWithFilter("Код", codeFilter));
        grid.addColumn(Cfo::getName)
                .setHeader(columnHeaderWithFilter("Наименование", nameFilter));

        Supplier<List<Cfo>> loader = () -> {
            String codeValue = normalizeFilterValue(codeFilter.getValue());
            String nameValue = normalizeFilterValue(nameFilter.getValue());
            return cfoRepository.findAll().stream()
                    .filter(item -> matchesCfoFilters(item, codeValue, nameValue))
                    .toList();
        };

        Runnable[] refreshHolder = new Runnable[1];

        VerticalLayout layout = genericGrid(Cfo.class, grid,
                loader,
                cfoRepository::save,
                cfoRepository::delete,
                (selected, refresh) -> {
                    Cfo bean = selected != null ? selected : new Cfo();
                    Dialog d = new Dialog("ЦФО");
                    Binder<Cfo> binder = new Binder<>(Cfo.class);
                    TextField code = new TextField("Код");
                    TextField name = new TextField("Наименование");
                    binder.bind(code, Cfo::getCode, Cfo::setCode);
                    binder.bind(name, Cfo::getName, Cfo::setName);
                    binder.setBean(bean);
                    Button save = new Button("Сохранить", e -> {
                        cfoRepository.save(binder.getBean());
                        Runnable targetRefresh = refreshHolder[0] != null ? refreshHolder[0] : refresh;
                        targetRefresh.run();
                        d.close();
                    });
                    Button del = new Button("Удалить", e -> {
                        if (bean.getId() != null) {
                            cfoRepository.delete(bean);
                        }
                        Runnable targetRefresh = refreshHolder[0] != null ? refreshHolder[0] : refresh;
                        targetRefresh.run();
                        d.close();
                    });
                    del.addThemeVariants(ButtonVariant.LUMO_ERROR);
                    Button close = new Button("Закрыть", e -> d.close());
                    d.add(new FormLayout(code, name), new HorizontalLayout(save, del, close));
                    return d;
                },
                refresh -> refreshHolder[0] = refresh);

        codeFilter.addValueChangeListener(e -> {
            if (refreshHolder[0] != null) {
                refreshHolder[0].run();
            }
        });
        nameFilter.addValueChangeListener(e -> {
            if (refreshHolder[0] != null) {
                refreshHolder[0].run();
            }
        });

        return layout;
    }

    private VerticalLayout mvzGrid() {
        Grid<Mvz> grid = new Grid<>(Mvz.class, false);
        grid.addColumn(Mvz::getCode).setHeader("Код");
        grid.addColumn(Mvz::getName).setHeader("Наименование");
        grid.addColumn(item -> item.getCfo() != null ? item.getCfo().getName() : "—").setHeader("ЦФО");

        return genericGrid(Mvz.class, grid,
                () -> mvzRepository.findAll(),
                mvzRepository::save,
                mvzRepository::delete,
                (selected, refresh) -> {
                    Mvz bean = selected != null ? selected : new Mvz();
                    Dialog d = new Dialog("МВЗ");
                    Binder<Mvz> binder = new Binder<>(Mvz.class);
                    TextField code = new TextField("Код");
                    TextField name = new TextField("Наименование");
                    ComboBox<Cfo> cfo = new ComboBox<>("ЦФО");
                    cfo.setItems(cfoRepository.findAll());
                    cfo.setItemLabelGenerator(Cfo::getName);
                    binder.bind(code, Mvz::getCode, Mvz::setCode);
                    binder.bind(name, Mvz::getName, Mvz::setName);
                    binder.bind(cfo, Mvz::getCfo, Mvz::setCfo);
                    binder.setBean(bean);
                    Button save = new Button("Сохранить", e -> { mvzRepository.save(binder.getBean()); refresh.run(); d.close(); });
                    Button del = new Button("Удалить", e -> { if (bean.getId()!=null) mvzRepository.delete(bean); refresh.run(); d.close(); });
                    del.addThemeVariants(ButtonVariant.LUMO_ERROR);
                    Button close = new Button("Закрыть", e -> d.close());
                    d.add(new FormLayout(code, name, cfo), new HorizontalLayout(save, del, close));
                    return d;
                });
    }

    private VerticalLayout contractGrid() {
        Grid<Contract> grid = new Grid<>(Contract.class, false);
        grid.addColumn(Contract::getName).setHeader("Наименование");
        grid.addColumn(Contract::getInternalNumber).setHeader("№ внутренний");
        grid.addColumn(Contract::getExternalNumber).setHeader("№ внешний");
        grid.addColumn(c -> c.getContractDate() != null ? c.getContractDate().toString() : "—").setHeader("Дата");
        grid.addColumn(Contract::getResponsible).setHeader("Ответственный");

        return genericGrid(Contract.class, grid,
                contractService::findAll,
                contractService::save,
                contractService::delete,
                (selected, refresh) -> {
                    Contract bean = selected != null ? selected : new Contract();
                    Dialog d = new Dialog("Договор");
                    Binder<Contract> binder = new Binder<>(Contract.class);
                    TextField name = new TextField("Наименование");
                    TextField inum = new TextField("№ внутренний");
                    TextField exnum = new TextField("№ внешний");
                    DatePicker date = new DatePicker("Дата");
                    TextField resp = new TextField("Ответственный (ФИО)");

                    if (bean.getContractDate() != null) {
                        date.setValue(bean.getContractDate());
                    }

                    binder.forField(name).bind(Contract::getName, Contract::setName);
                    binder.bind(inum, Contract::getInternalNumber, Contract::setInternalNumber);
                    binder.bind(exnum, Contract::getExternalNumber, Contract::setExternalNumber);
                    binder.bind(date, Contract::getContractDate, Contract::setContractDate);
                    binder.bind(resp, Contract::getResponsible, Contract::setResponsible);
                    binder.setBean(bean);

                    Button save = new Button("Сохранить", e -> {
                        contractService.save(binder.getBean());
                        refresh.run();
                        d.close();
                    });
                    Button del = new Button("Удалить", e -> { if (bean.getId()!=null) contractService.delete(bean); refresh.run(); d.close(); });
                    del.addThemeVariants(ButtonVariant.LUMO_ERROR);
                    Button close = new Button("Закрыть", e -> d.close());
                    d.add(new FormLayout(name, inum, exnum, date, resp), new HorizontalLayout(save, del, close));
                    return d;
                });
    }
}
