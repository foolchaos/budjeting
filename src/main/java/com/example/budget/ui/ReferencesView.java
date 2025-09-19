package com.example.budget.ui;

import com.example.budget.domain.*;
import com.example.budget.repo.*;
import com.example.budget.service.BdzService;
import com.example.budget.service.BoService;
import com.example.budget.service.ExcelImportException;
import com.example.budget.service.ExcelImportResult;
import com.example.budget.service.CfoService;
import com.example.budget.service.CfoTwoService;
import com.example.budget.service.ContractService;
import com.example.budget.service.CounterpartyService;
import com.example.budget.service.ProcurementMethodService;
import com.example.budget.service.ZgdService;
import com.example.budget.service.MvzService;
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
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.UploadI18N;
import com.vaadin.flow.component.upload.receivers.FileBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.spring.annotation.UIScope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
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
import java.util.function.Consumer;
import java.util.function.Supplier;

@Component
@UIScope
public class ReferencesView extends SplitLayout {

    private static final long IMPORT_MAX_FILE_SIZE = 10 * 1024 * 1024L;

    private final BdzService bdzService;
    private final BoService boService;
    private final ZgdService zgdService;
    private final CfoService cfoService;
    private final CfoTwoService cfoTwoService;
    private final CfoRepository cfoRepository;
    private final CfoTwoRepository cfoTwoRepository;
    private final MvzService mvzService;
    private final ContractService contractService;
    private final CounterpartyService counterpartyService;
    private final ProcurementMethodService procurementMethodService;

    private final ListBox<String> leftMenu = new ListBox<>();
    private final Div rightPanel = new Div();

    public ReferencesView(BdzService bdzService, BoService boService, ZgdService zgdService,
                          CfoService cfoService, CfoTwoService cfoTwoService, CfoRepository cfoRepository, CfoTwoRepository cfoTwoRepository,
                          MvzService mvzService, ContractService contractService, CounterpartyService counterpartyService,
                          ProcurementMethodService procurementMethodService) {
        this.bdzService = bdzService;
        this.boService = boService;
        this.zgdService = zgdService;
        this.cfoService = cfoService;
        this.cfoTwoService = cfoTwoService;
        this.cfoRepository = cfoRepository;
        this.cfoTwoRepository = cfoTwoRepository;
        this.mvzService = mvzService;
        this.contractService = contractService;
        this.counterpartyService = counterpartyService;
        this.procurementMethodService = procurementMethodService;

        setSizeFull();
        leftMenu.setItems("ЦФО I", "ЦФО II", "БДЗ", "БО", "ЗГД", "МВЗ", "Контрагент", "Договор", "Способ закупки");
        leftMenu.setValue("ЦФО I");
        leftMenu.addValueChangeListener(e -> renderRight(e.getValue()));

        rightPanel.setSizeFull();
        setOrientation(SplitLayout.Orientation.HORIZONTAL);
        setSplitterPosition(20);
        addToPrimary(leftMenu);
        addToSecondary(rightPanel);

        renderRight("ЦФО I");
    }

    private void renderRight(String name) {
        rightPanel.removeAll();
        switch (name) {
            case "ЦФО I" -> rightPanel.add(cfoOneGrid());
            case "ЦФО II" -> rightPanel.add(cfoTwoGrid());
            case "БДЗ" -> rightPanel.add(bdzTree());
            case "БО" -> rightPanel.add(boGrid());
            case "ЗГД" -> rightPanel.add(zgdGrid());
            case "МВЗ" -> rightPanel.add(mvzGrid());
            case "Контрагент" -> rightPanel.add(counterpartyGrid());
            case "Договор" -> rightPanel.add(contractGrid());
            case "Способ закупки" -> rightPanel.add(procurementMethodGrid());
        }
    }

    // ==== BDZ Tree ====
    private VerticalLayout bdzTree() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        Button create = new Button("Создать");
        Button delete = new Button("Удалить выбранные");
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);

        final FileBuffer importBuffer = new FileBuffer();
        Upload importUpload = new Upload(importBuffer);
        importUpload.setAcceptedFileTypes(".xlsx");
        importUpload.setMaxFiles(1);
        importUpload.setMaxFileSize((int) IMPORT_MAX_FILE_SIZE);
        importUpload.setDropAllowed(false);
        importUpload.setAutoUpload(true);
        importUpload.getStyle().set("padding", "0");
        Button importButton = new Button("Импорт");
        importButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        importUpload.setUploadButton(importButton);

        UploadI18N importI18n = new UploadI18N();
        importI18n.setError(new UploadI18N.Error()
                .setFileIsTooBig("Файл превышает 10 МБ")
                .setIncorrectFileType("Неверный тип файла. Ожидается .xlsx")
                .setTooManyFiles("Можно загрузить только один файл"));
        importUpload.setI18n(importI18n);

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

        TextField cfoFilter = new TextField();
        cfoFilter.setValueChangeMode(ValueChangeMode.EAGER);
        cfoFilter.setClearButtonVisible(true);

        tree.addHierarchyColumn(Bdz::getCode)
                .setHeader(columnHeaderWithFilter("Код", codeFilter));
        tree.addColumn(Bdz::getName)
                .setHeader(columnHeaderWithFilter("Наименование", nameFilter));
        tree.addColumn(item -> {
            Cfo cfo = item.getCfo();
            if (cfo == null) {
                return "—";
            }
            String code = cfo.getCode() != null ? cfo.getCode() : "";
            String name = cfo.getName() != null ? cfo.getName() : "";
            return (code + " " + name).trim();
        }).setHeader(columnHeaderWithFilter("ЦФО I", cfoFilter));

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
            String cfoValue = normalizeFilterValue(cfoFilter.getValue());
            boolean hasFilter = (codeValue != null) || (nameValue != null) || (cfoValue != null);

            if (!hasFilter) {
                filteredMode.set(false);
                roots.addAll(bdzService.findRoots());
            } else {
                filteredMode.set(true);
                List<Bdz> allItems = bdzService.findAll();
                Set<Long> includedIds = new LinkedHashSet<>();

                for (Bdz item : allItems) {
                    if (matchesBdzFilters(item, codeValue, nameValue, cfoValue)) {
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
        cfoFilter.addValueChangeListener(e -> reload.run());

        importUpload.addFileRejectedListener(event ->
                showErrorNotification(event.getErrorMessage()));
        importUpload.addFailedListener(event ->
                showErrorNotification("Загрузка файла прервана"));
        importUpload.addSucceededListener(event -> {
            Path tempFile = importBuffer.getFileData().getFile().toPath();
            try {
                ExcelImportResult result = bdzService.importFromXlsx(tempFile);
                showSuccessNotification(String.format(
                        "Импорт БДЗ завершён: создано %d, обновлено %d, обработано %d",
                        result.created(), result.updated(), result.processed()));
                reload.run();
            } catch (ExcelImportException ex) {
                showErrorNotification(ex.getMessage());
            } catch (Exception ex) {
                showErrorNotification("Не удалось импортировать файл");
            } finally {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                }
                importUpload.clearFileList();
            }
        });

        HorizontalLayout pagination = new HorizontalLayout(prev, next, pageInfo, pageSizeSelect);
        pagination.setAlignItems(Alignment.CENTER);
        pagination.setWidthFull();
        pageInfo.getStyle().set("margin-left", "auto");
        pageInfo.getStyle().set("margin-right", "var(--lumo-space-m)");

        HorizontalLayout actions = new HorizontalLayout(create, delete, importUpload);
        actions.setAlignItems(Alignment.CENTER);
        actions.setWidthFull();
        importUpload.getStyle().set("margin-left", "auto");

        layout.add(actions, tree, pagination);
        layout.setFlexGrow(1, tree);
        reload.run();
        return layout;
    }

    private boolean matchesBdzFilters(Bdz item,
                                      String normalizedCodeFilter,
                                      String normalizedNameFilter,
                                      String normalizedCfoFilter) {
        boolean codeMatches = containsNormalized(item.getCode(), normalizedCodeFilter);
        boolean nameMatches = containsNormalized(item.getName(), normalizedNameFilter);
        boolean cfoMatches;
        if (normalizedCfoFilter == null) {
            cfoMatches = true;
        } else if (item.getCfo() == null) {
            cfoMatches = false;
        } else {
            cfoMatches = containsNormalized(item.getCfo().getCode(), normalizedCfoFilter)
                    || containsNormalized(item.getCfo().getName(), normalizedCfoFilter);
        }
        return codeMatches && nameMatches && cfoMatches;
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

    private boolean matchesCodeNameFilters(String code, String name,
                                           String normalizedCodeFilter,
                                           String normalizedNameFilter) {
        return containsNormalized(code, normalizedCodeFilter)
                && containsNormalized(name, normalizedNameFilter);
    }

    private boolean matchesCfoOneFilters(Cfo item, String normalizedCodeFilter, String normalizedNameFilter) {
        if (item == null) {
            return false;
        }
        return matchesCodeNameFilters(item.getCode(), item.getName(), normalizedCodeFilter, normalizedNameFilter);
    }

    private boolean matchesCfoTwoFilters(CfoTwo item, String normalizedCodeFilter, String normalizedNameFilter) {
        if (item == null) {
            return false;
        }
        return matchesCodeNameFilters(item.getCode(), item.getName(), normalizedCodeFilter, normalizedNameFilter);
    }

    private boolean matchesMvzFilters(Mvz item,
                                      String normalizedCodeFilter,
                                      String normalizedNameFilter,
                                      String normalizedCfoFilter) {
        if (!containsNormalized(item.getCode(), normalizedCodeFilter)) {
            return false;
        }
        if (!containsNormalized(item.getName(), normalizedNameFilter)) {
            return false;
        }

        if (normalizedCfoFilter == null) {
            return true;
        }

        if (item.getCfo() == null) {
            return false;
        }

        return containsNormalized(item.getCfo().getCode(), normalizedCfoFilter)
                || containsNormalized(item.getCfo().getName(), normalizedCfoFilter);
    }

    private boolean matchesContractFilters(Contract item,
                                           String normalizedNameFilter,
                                           String normalizedInternalNumberFilter,
                                           String normalizedExternalNumberFilter,
                                           String normalizedResponsibleFilter,
                                           LocalDate fromDateFilter,
                                           LocalDate toDateFilter) {
        if (!containsNormalized(item.getName(), normalizedNameFilter)) {
            return false;
        }
        if (!containsNormalized(item.getInternalNumber(), normalizedInternalNumberFilter)) {
            return false;
        }
        if (!containsNormalized(item.getExternalNumber(), normalizedExternalNumberFilter)) {
            return false;
        }
        if (!containsNormalized(item.getResponsible(), normalizedResponsibleFilter)) {
            return false;
        }

        LocalDate date = item.getContractDate();
        if (fromDateFilter != null) {
            if (date == null || date.isBefore(fromDateFilter)) {
                return false;
            }
        }
        if (toDateFilter != null) {
            if (date == null || date.isAfter(toDateFilter)) {
                return false;
            }
        }
        return true;
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

    private boolean matchesFilter(String value, String normalizedFilter) {
        return containsNormalized(value, normalizedFilter);
    }

    private void openBdzCard(Bdz entity, Runnable refresh) {
        Bdz bean = entity.getId() != null ? bdzService.findById(entity.getId()) : entity;

        Dialog dlg = new Dialog("Статья БДЗ");
        dlg.setWidth("500px");
        Binder<Bdz> binder = new Binder<>(Bdz.class);

        TextField code = new TextField("Код");
        TextField name = new TextField("Наименование");
        ComboBox<Cfo> cfoField = new ComboBox<>("ЦФО I");
        List<Cfo> cfoOptions = new ArrayList<>(cfoRepository.findAll());
        cfoField.setItems(cfoOptions);
        cfoField.setItemLabelGenerator(item -> {
            String codePart = item.getCode() != null ? item.getCode() : "";
            String namePart = item.getName() != null ? item.getName() : "";
            return (codePart + " " + namePart).trim();
        });
        cfoField.setClearButtonVisible(true);
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

        if (bean.getCfo() != null) {
            Long cfoId = bean.getCfo().getId();
            if (cfoId != null) {
                Cfo selectedCfo = cfoOptions.stream()
                        .filter(c -> Objects.equals(c.getId(), cfoId))
                        .findFirst()
                        .orElse(null);
                if (selectedCfo != null) {
                    bean.setCfo(selectedCfo);
                } else {
                    cfoOptions.add(bean.getCfo());
                }
            }
        }

        binder.bind(code, Bdz::getCode, Bdz::setCode);
        binder.bind(name, Bdz::getName, Bdz::setName);
        binder.bind(cfoField, Bdz::getCfo, Bdz::setCfo);
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

        FormLayout form = new FormLayout(code, name, cfoField, parent);
        dlg.add(form, new HorizontalLayout(save, delete, close));
        dlg.open();
    }

    // ==== Generic helpers for other refs ====
    private <T> VerticalLayout genericGrid(Class<T> type, Grid<T> grid,
                                           Supplier<List<T>> loader,
                                           java.util.function.Function<T, T> saver,
                                           java.util.function.Consumer<T> deleter,
                                           BiFunction<T, Runnable, Dialog> editorFactory) {
        return genericGrid(type, grid, loader, saver, deleter, editorFactory, null, null);
    }

    private <T> VerticalLayout genericGrid(Class<T> type, Grid<T> grid,
                                           Supplier<List<T>> loader,
                                           java.util.function.Function<T, T> saver,
                                           java.util.function.Consumer<T> deleter,
                                           BiFunction<T, Runnable, Dialog> editorFactory,
                                           java.util.function.Consumer<Runnable> refreshObserver) {
        return genericGrid(type, grid, loader, saver, deleter, editorFactory, refreshObserver, null);
    }

    private <T> VerticalLayout genericGrid(Class<T> type, Grid<T> grid,
                                           Supplier<List<T>> loader,
                                           java.util.function.Function<T, T> saver,
                                           java.util.function.Consumer<T> deleter,
                                           BiFunction<T, Runnable, Dialog> editorFactory,
                                           java.util.function.Consumer<Runnable> refreshObserver,
                                           Consumer<GenericGridContext> actionsCustomizer) {
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

        HorizontalLayout actions = new HorizontalLayout();
        actions.setAlignItems(Alignment.CENTER);
        actions.add(create, delete);
        HorizontalLayout pagination = new HorizontalLayout(prev, next, pageInfo, pageSizeSelect);
        pagination.setAlignItems(Alignment.CENTER);
        pagination.setWidthFull();
        pageInfo.getStyle().set("margin-left", "auto");
        pageInfo.getStyle().set("margin-right", "var(--lumo-space-m)");

        if (actionsCustomizer != null) {
            actionsCustomizer.accept(new GenericGridContext(actions, create, delete, refresh));
        }

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
                refresh -> refreshHolder[0] = refresh,
                context -> {
                    FileBuffer buffer = new FileBuffer();
                    Upload upload = new Upload(buffer);
                    upload.setAcceptedFileTypes(".xlsx");
                    upload.setMaxFiles(1);
                    upload.setMaxFileSize((int) IMPORT_MAX_FILE_SIZE);
                    upload.setDropAllowed(false);
                    upload.setAutoUpload(true);
                    upload.getStyle().set("padding", "0");
                    Button importButton = new Button("Импорт");
                    importButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                    upload.setUploadButton(importButton);

                    UploadI18N i18n = new UploadI18N();
                    i18n.setError(new UploadI18N.Error()
                            .setFileIsTooBig("Файл превышает 10 МБ")
                            .setIncorrectFileType("Неверный тип файла. Ожидается .xlsx")
                            .setTooManyFiles("Можно загрузить только один файл"));
                    upload.setI18n(i18n);

                    upload.addFileRejectedListener(event ->
                            showErrorNotification(event.getErrorMessage()));
                    upload.addFailedListener(event ->
                            showErrorNotification("Загрузка файла прервана"));
                    upload.addSucceededListener(event -> {
                        Path tempFile = buffer.getFileData().getFile().toPath();
                        try {
                            ExcelImportResult result = boService.importFromXlsx(tempFile);
                            showSuccessNotification(String.format(
                                    "Импорт БО завершён: создано %d, обновлено %d, обработано %d",
                                    result.created(), result.updated(), result.processed()));
                            Runnable targetRefresh = refreshHolder[0] != null
                                    ? refreshHolder[0]
                                    : context.refresh();
                            if (targetRefresh != null) {
                                targetRefresh.run();
                            }
                        } catch (ExcelImportException ex) {
                            showErrorNotification(ex.getMessage());
                        } catch (Exception ex) {
                            showErrorNotification("Не удалось импортировать файл");
                        } finally {
                            try {
                                Files.deleteIfExists(tempFile);
                            } catch (IOException ignored) {
                            }
                            upload.clearFileList();
                        }
                    });

                    context.actions().setWidthFull();
                    upload.getStyle().set("margin-left", "auto");
                    context.actions().add(upload);
                });

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
                refresh -> refreshHolder[0] = refresh,
                context -> {
                    FileBuffer buffer = new FileBuffer();
                    Upload upload = new Upload(buffer);
                    upload.setAcceptedFileTypes(".xlsx");
                    upload.setMaxFiles(1);
                    upload.setMaxFileSize((int) IMPORT_MAX_FILE_SIZE);
                    upload.setDropAllowed(false);
                    upload.setAutoUpload(true);
                    upload.getStyle().set("padding", "0");
                    Button importButton = new Button("Импорт");
                    importButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                    upload.setUploadButton(importButton);

                    UploadI18N i18n = new UploadI18N();
                    i18n.setError(new UploadI18N.Error()
                            .setFileIsTooBig("Файл превышает 10 МБ")
                            .setIncorrectFileType("Неверный тип файла. Ожидается .xlsx")
                            .setTooManyFiles("Можно загрузить только один файл"));
                    upload.setI18n(i18n);

                    upload.addFileRejectedListener(event ->
                            showErrorNotification(event.getErrorMessage()));
                    upload.addFailedListener(event ->
                            showErrorNotification("Загрузка файла прервана"));
                    upload.addSucceededListener(event -> {
                        Path tempFile = buffer.getFileData().getFile().toPath();
                        try {
                            ExcelImportResult result = zgdService.importFromXlsx(tempFile);
                            showSuccessNotification(String.format(
                                    "Импорт ЗГД завершён: создано %d, обновлено %d, обработано %d",
                                    result.created(), result.updated(), result.processed()));
                            Runnable targetRefresh = refreshHolder[0] != null
                                    ? refreshHolder[0]
                                    : context.refresh();
                            if (targetRefresh != null) {
                                targetRefresh.run();
                            }
                        } catch (ExcelImportException ex) {
                            showErrorNotification(ex.getMessage());
                        } catch (Exception ex) {
                            showErrorNotification("Не удалось импортировать файл");
                        } finally {
                            try {
                                Files.deleteIfExists(tempFile);
                            } catch (IOException ignored) {
                            }
                            upload.clearFileList();
                        }
                    });

                    context.actions().setWidthFull();
                    upload.getStyle().set("margin-left", "auto");
                    context.actions().add(upload);
                });

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

    private VerticalLayout cfoOneGrid() {
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
                    .filter(item -> matchesCfoOneFilters(item, codeValue, nameValue))
                    .toList();
        };

        Runnable[] refreshHolder = new Runnable[1];

        VerticalLayout layout = genericGrid(Cfo.class, grid,
                loader,
                cfoRepository::save,
                cfoRepository::delete,
                (selected, refresh) -> {
                    Cfo bean = selected != null ? selected : new Cfo();
                    Dialog d = new Dialog("ЦФО I");
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
                refresh -> refreshHolder[0] = refresh,
                context -> {
                    FileBuffer buffer = new FileBuffer();
                    Upload upload = new Upload(buffer);
                    upload.setAcceptedFileTypes(".xlsx");
                    upload.setMaxFiles(1);
                    upload.setMaxFileSize((int) IMPORT_MAX_FILE_SIZE);
                    upload.setDropAllowed(false);
                    upload.setAutoUpload(true);
                    upload.getStyle().set("padding", "0");
                    Button importButton = new Button("Импорт");
                    importButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                    upload.setUploadButton(importButton);

                    UploadI18N i18n = new UploadI18N();
                    i18n.setError(new UploadI18N.Error()
                            .setFileIsTooBig("Файл превышает 10 МБ")
                            .setIncorrectFileType("Неверный тип файла. Ожидается .xlsx")
                            .setTooManyFiles("Можно загрузить только один файл"));
                    upload.setI18n(i18n);

                    upload.addFileRejectedListener(event ->
                            showErrorNotification(event.getErrorMessage()));
                    upload.addFailedListener(event ->
                            showErrorNotification("Загрузка файла прервана"));
                    upload.addSucceededListener(event -> {
                        Path tempFile = buffer.getFileData().getFile().toPath();
                        try {
                            ExcelImportResult result = cfoService.importFromXlsx(tempFile);
                            showSuccessNotification(String.format(
                                    "Импорт завершён: создано %d, обновлено %d, обработано %d",
                                    result.created(), result.updated(), result.processed()));
                            Runnable targetRefresh = refreshHolder[0] != null
                                    ? refreshHolder[0]
                                    : context.refresh();
                            if (targetRefresh != null) {
                                targetRefresh.run();
                            }
                        } catch (ExcelImportException ex) {
                            showErrorNotification(ex.getMessage());
                        } catch (Exception ex) {
                            showErrorNotification("Не удалось импортировать файл");
                        } finally {
                            try {
                                Files.deleteIfExists(tempFile);
                            } catch (IOException ignored) {
                            }
                            upload.clearFileList();
                        }
                    });

                    context.actions().setWidthFull();
                    upload.getStyle().set("margin-left", "auto");
                    context.actions().add(upload);
                });

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

    private VerticalLayout cfoTwoGrid() {
        Grid<CfoTwo> grid = new Grid<>(CfoTwo.class, false);

        TextField codeFilter = new TextField();
        codeFilter.setValueChangeMode(ValueChangeMode.EAGER);
        codeFilter.setClearButtonVisible(true);

        TextField nameFilter = new TextField();
        nameFilter.setValueChangeMode(ValueChangeMode.EAGER);
        nameFilter.setClearButtonVisible(true);

        grid.addColumn(CfoTwo::getCode)
                .setHeader(columnHeaderWithFilter("Код", codeFilter));
        grid.addColumn(CfoTwo::getName)
                .setHeader(columnHeaderWithFilter("Наименование", nameFilter));

        Supplier<List<CfoTwo>> loader = () -> {
            String codeValue = normalizeFilterValue(codeFilter.getValue());
            String nameValue = normalizeFilterValue(nameFilter.getValue());
            return cfoTwoRepository.findAll().stream()
                    .filter(item -> matchesCfoTwoFilters(item, codeValue, nameValue))
                    .toList();
        };

        Runnable[] refreshHolder = new Runnable[1];

        VerticalLayout layout = genericGrid(CfoTwo.class, grid,
                loader,
                cfoTwoRepository::save,
                cfoTwoRepository::delete,
                (selected, refresh) -> {
                    CfoTwo bean = selected != null ? selected : new CfoTwo();
                    Dialog d = new Dialog("ЦФО II");
                    Binder<CfoTwo> binder = new Binder<>(CfoTwo.class);
                    TextField code = new TextField("Код");
                    TextField name = new TextField("Наименование");
                    binder.bind(code, CfoTwo::getCode, CfoTwo::setCode);
                    binder.bind(name, CfoTwo::getName, CfoTwo::setName);
                    binder.setBean(bean);
                    Button save = new Button("Сохранить", e -> {
                        cfoTwoRepository.save(binder.getBean());
                        Runnable targetRefresh = refreshHolder[0] != null ? refreshHolder[0] : refresh;
                        targetRefresh.run();
                        d.close();
                    });
                    Button del = new Button("Удалить", e -> {
                        if (bean.getId() != null) {
                            cfoTwoRepository.delete(bean);
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
                refresh -> refreshHolder[0] = refresh,
                context -> {
                    FileBuffer buffer = new FileBuffer();
                    Upload upload = new Upload(buffer);
                    upload.setAcceptedFileTypes(".xlsx");
                    upload.setMaxFiles(1);
                    upload.setMaxFileSize((int) IMPORT_MAX_FILE_SIZE);
                    upload.setDropAllowed(false);
                    upload.setAutoUpload(true);
                    upload.getStyle().set("padding", "0");
                    Button importButton = new Button("Импорт");
                    importButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                    upload.setUploadButton(importButton);

                    UploadI18N i18n = new UploadI18N();
                    i18n.setError(new UploadI18N.Error()
                            .setFileIsTooBig("Файл превышает 10 МБ")
                            .setIncorrectFileType("Неверный тип файла. Ожидается .xlsx")
                            .setTooManyFiles("Можно загрузить только один файл"));
                    upload.setI18n(i18n);

                    upload.addFileRejectedListener(event ->
                            showErrorNotification(event.getErrorMessage()));
                    upload.addFailedListener(event ->
                            showErrorNotification("Загрузка файла прервана"));
                    upload.addSucceededListener(event -> {
                        Path tempFile = buffer.getFileData().getFile().toPath();
                        try {
                            ExcelImportResult result = cfoTwoService.importFromXlsx(tempFile);
                            showSuccessNotification(String.format(
                                    "Импорт ЦФО II завершён: создано %d, обновлено %d, обработано %d",
                                    result.created(), result.updated(), result.processed()));
                            Runnable targetRefresh = refreshHolder[0] != null
                                    ? refreshHolder[0]
                                    : context.refresh();
                            if (targetRefresh != null) {
                                targetRefresh.run();
                            }
                        } catch (ExcelImportException ex) {
                            showErrorNotification(ex.getMessage());
                        } catch (Exception ex) {
                            showErrorNotification("Не удалось импортировать файл");
                        } finally {
                            try {
                                Files.deleteIfExists(tempFile);
                            } catch (IOException ignored) {
                            }
                            upload.clearFileList();
                        }
                    });

                    context.actions().setWidthFull();
                    upload.getStyle().set("margin-left", "auto");
                    context.actions().add(upload);
                });

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

        TextField codeFilter = new TextField();
        codeFilter.setValueChangeMode(ValueChangeMode.EAGER);
        codeFilter.setClearButtonVisible(true);

        TextField nameFilter = new TextField();
        nameFilter.setValueChangeMode(ValueChangeMode.EAGER);
        nameFilter.setClearButtonVisible(true);

        TextField cfoFilter = new TextField();
        cfoFilter.setValueChangeMode(ValueChangeMode.EAGER);
        cfoFilter.setClearButtonVisible(true);

        grid.addColumn(Mvz::getCode)
                .setHeader(columnHeaderWithFilter("Код", codeFilter));
        grid.addColumn(Mvz::getName)
                .setHeader(columnHeaderWithFilter("Наименование", nameFilter));
        grid.addColumn(item -> {
            if (item.getCfo() == null) {
                return "—";
            }
            Cfo cfo = item.getCfo();
            String code = cfo.getCode() != null ? cfo.getCode() : "";
            String name = cfo.getName() != null ? cfo.getName() : "";
            return (code + " " + name).trim();
        }).setHeader(columnHeaderWithFilter("ЦФО I", cfoFilter));

        Supplier<List<Mvz>> loader = () -> {
            String codeValue = normalizeFilterValue(codeFilter.getValue());
            String nameValue = normalizeFilterValue(nameFilter.getValue());
            String cfoValue = normalizeFilterValue(cfoFilter.getValue());
            return mvzService.findAll().stream()
                    .filter(item -> matchesMvzFilters(item, codeValue, nameValue, cfoValue))
                    .toList();
        };

        Runnable[] refreshHolder = new Runnable[1];

        VerticalLayout layout = genericGrid(Mvz.class, grid,
                loader,
                mvzService::save,
                mvzService::delete,
                (selected, refresh) -> {
                    Mvz bean = selected != null ? selected : new Mvz();
                    Dialog d = new Dialog("МВЗ");
                    Binder<Mvz> binder = new Binder<>(Mvz.class);
                    TextField code = new TextField("Код");
                    TextField name = new TextField("Наименование");
                    ComboBox<Cfo> cfo = new ComboBox<>("ЦФО I");
                    cfo.setItems(cfoRepository.findAll());
                    cfo.setItemLabelGenerator(item -> {
                        String cfoCode = item.getCode() != null ? item.getCode() : "";
                        String cfoName = item.getName() != null ? item.getName() : "";
                        return (cfoCode + " " + cfoName).trim();
                    });
                    cfo.setClearButtonVisible(true);
                    binder.bind(code, Mvz::getCode, Mvz::setCode);
                    binder.bind(name, Mvz::getName, Mvz::setName);
                    binder.bind(cfo, Mvz::getCfo, Mvz::setCfo);
                    binder.setBean(bean);
                    Button save = new Button("Сохранить", e -> {
                        mvzService.save(binder.getBean());
                        Runnable targetRefresh = refreshHolder[0] != null ? refreshHolder[0] : refresh;
                        targetRefresh.run();
                        d.close();
                    });
                    Button del = new Button("Удалить", e -> {
                        if (bean.getId() != null) {
                            mvzService.delete(bean);
                        }
                        Runnable targetRefresh = refreshHolder[0] != null ? refreshHolder[0] : refresh;
                        targetRefresh.run();
                        d.close();
                    });
                    del.addThemeVariants(ButtonVariant.LUMO_ERROR);
                    Button close = new Button("Закрыть", e -> d.close());
                    d.add(new FormLayout(code, name, cfo), new HorizontalLayout(save, del, close));
                    return d;
                },
                refresh -> refreshHolder[0] = refresh,
                context -> {
                    FileBuffer buffer = new FileBuffer();
                    Upload upload = new Upload(buffer);
                    upload.setAcceptedFileTypes(".xlsx");
                    upload.setMaxFiles(1);
                    upload.setMaxFileSize((int) IMPORT_MAX_FILE_SIZE);
                    upload.setDropAllowed(false);
                    upload.setAutoUpload(true);
                    upload.getStyle().set("padding", "0");
                    Button importButton = new Button("Импорт");
                    importButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                    upload.setUploadButton(importButton);

                    UploadI18N i18n = new UploadI18N();
                    i18n.setError(new UploadI18N.Error()
                            .setFileIsTooBig("Файл превышает 10 МБ")
                            .setIncorrectFileType("Неверный тип файла. Ожидается .xlsx")
                            .setTooManyFiles("Можно загрузить только один файл"));
                    upload.setI18n(i18n);

                    upload.addFileRejectedListener(event ->
                            showErrorNotification(event.getErrorMessage()));
                    upload.addFailedListener(event ->
                            showErrorNotification("Загрузка файла прервана"));
                    upload.addSucceededListener(event -> {
                        Path tempFile = buffer.getFileData().getFile().toPath();
                        try {
                            ExcelImportResult result = mvzService.importFromXlsx(tempFile);
                            showSuccessNotification(String.format(
                                    "Импорт МВЗ завершён: создано %d, обновлено %d, обработано %d",
                                    result.created(), result.updated(), result.processed()));
                            Runnable targetRefresh = refreshHolder[0] != null
                                    ? refreshHolder[0]
                                    : context.refresh();
                            if (targetRefresh != null) {
                                targetRefresh.run();
                            }
                        } catch (ExcelImportException ex) {
                            showErrorNotification(ex.getMessage());
                        } catch (Exception ex) {
                            showErrorNotification("Не удалось импортировать файл");
                        } finally {
                            try {
                                Files.deleteIfExists(tempFile);
                            } catch (IOException ignored) {
                            }
                            upload.clearFileList();
                        }
                    });

                    context.actions().setWidthFull();
                    upload.getStyle().set("margin-left", "auto");
                    context.actions().add(upload);
                });

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
        cfoFilter.addValueChangeListener(e -> {
            if (refreshHolder[0] != null) {
                refreshHolder[0].run();
            }
        });

        return layout;
    }

    private VerticalLayout procurementMethodGrid() {
        Grid<ProcurementMethod> grid = new Grid<>(ProcurementMethod.class, false);

        TextField nameFilter = new TextField();
        nameFilter.setValueChangeMode(ValueChangeMode.EAGER);
        nameFilter.setClearButtonVisible(true);

        grid.addColumn(ProcurementMethod::getName)
                .setHeader(columnHeaderWithFilter("Наименование", nameFilter));

        Supplier<List<ProcurementMethod>> loader = () -> {
            String nameValue = normalizeFilterValue(nameFilter.getValue());
            return procurementMethodService.findAll().stream()
                    .filter(item -> matchesFilter(item.getName(), nameValue))
                    .toList();
        };

        Runnable[] refreshHolder = new Runnable[1];

        VerticalLayout layout = genericGrid(ProcurementMethod.class, grid,
                loader,
                procurementMethodService::save,
                procurementMethodService::delete,
                (selected, refresh) -> {
                    ProcurementMethod bean = selected != null ? selected : new ProcurementMethod();
                    Dialog dialog = new Dialog("Способ закупки");
                    Binder<ProcurementMethod> binder = new Binder<>(ProcurementMethod.class);
                    TextField name = new TextField("Наименование");
                    name.setWidthFull();
                    binder.forField(name).bind(ProcurementMethod::getName, ProcurementMethod::setName);
                    binder.setBean(bean);

                    Button save = new Button("Сохранить", e -> {
                        procurementMethodService.save(binder.getBean());
                        Runnable targetRefresh = refreshHolder[0] != null ? refreshHolder[0] : refresh;
                        targetRefresh.run();
                        dialog.close();
                    });
                    Button delete = new Button("Удалить", e -> {
                        if (bean.getId() != null) {
                            procurementMethodService.delete(bean);
                        }
                        Runnable targetRefresh = refreshHolder[0] != null ? refreshHolder[0] : refresh;
                        targetRefresh.run();
                        dialog.close();
                    });
                    delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
                    Button close = new Button("Закрыть", e -> dialog.close());
                    dialog.add(new FormLayout(name), new HorizontalLayout(save, delete, close));
                    return dialog;
                },
                refresh -> refreshHolder[0] = refresh,
                context -> {
                    FileBuffer buffer = new FileBuffer();
                    Upload upload = new Upload(buffer);
                    upload.setAcceptedFileTypes(".xlsx");
                    upload.setMaxFiles(1);
                    upload.setMaxFileSize((int) IMPORT_MAX_FILE_SIZE);
                    upload.setDropAllowed(false);
                    upload.setAutoUpload(true);
                    upload.getStyle().set("padding", "0");
                    Button importButton = new Button("Импорт");
                    importButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                    upload.setUploadButton(importButton);

                    UploadI18N i18n = new UploadI18N();
                    i18n.setError(new UploadI18N.Error()
                            .setFileIsTooBig("Файл превышает 10 МБ")
                            .setIncorrectFileType("Неверный тип файла. Ожидается .xlsx")
                            .setTooManyFiles("Можно загрузить только один файл"));
                    upload.setI18n(i18n);

                    upload.addFileRejectedListener(event ->
                            showErrorNotification(event.getErrorMessage()));
                    upload.addFailedListener(event ->
                            showErrorNotification("Загрузка файла прервана"));
                    upload.addSucceededListener(event -> {
                        Path tempFile = buffer.getFileData().getFile().toPath();
                        try {
                            ExcelImportResult result = procurementMethodService.importFromXlsx(tempFile);
                            showSuccessNotification(String.format(
                                    "Импорт способов закупки завершён: создано %d, обновлено %d, обработано %d",
                                    result.created(), result.updated(), result.processed()));
                            Runnable targetRefresh = refreshHolder[0] != null
                                    ? refreshHolder[0]
                                    : context.refresh();
                            if (targetRefresh != null) {
                                targetRefresh.run();
                            }
                        } catch (ExcelImportException ex) {
                            showErrorNotification(ex.getMessage());
                        } catch (Exception ex) {
                            showErrorNotification("Не удалось импортировать файл");
                        } finally {
                            try {
                                Files.deleteIfExists(tempFile);
                            } catch (IOException ignored) {
                            }
                            upload.clearFileList();
                        }
                    });

                    context.actions().setWidthFull();
                    upload.getStyle().set("margin-left", "auto");
                    context.actions().add(upload);
                });

        nameFilter.addValueChangeListener(e -> {
            if (refreshHolder[0] != null) {
                refreshHolder[0].run();
            }
        });

        return layout;
    }

    private VerticalLayout counterpartyGrid() {
        Grid<Counterparty> grid = new Grid<>(Counterparty.class, false);

        TextField nameFilter = new TextField();
        nameFilter.setValueChangeMode(ValueChangeMode.EAGER);
        nameFilter.setClearButtonVisible(true);

        grid.addColumn(Counterparty::getLegalEntityName)
                .setHeader(columnHeaderWithFilter("Наименование юридического лица", nameFilter));

        Supplier<List<Counterparty>> loader = () -> {
            String nameValue = normalizeFilterValue(nameFilter.getValue());
            return counterpartyService.findAll().stream()
                    .filter(item -> matchesFilter(item.getLegalEntityName(), nameValue))
                    .toList();
        };

        Runnable[] refreshHolder = new Runnable[1];

        VerticalLayout layout = genericGrid(Counterparty.class, grid,
                loader,
                counterpartyService::save,
                counterpartyService::delete,
                (selected, refresh) -> {
                    Counterparty bean = selected != null ? selected : new Counterparty();
                    Dialog d = new Dialog("Контрагент");
                    Binder<Counterparty> binder = new Binder<>(Counterparty.class);
                    TextField name = new TextField("Наименование юридического лица");
                    name.setWidthFull();
                    binder.forField(name).bind(Counterparty::getLegalEntityName, Counterparty::setLegalEntityName);
                    binder.setBean(bean);

                    Button save = new Button("Сохранить", e -> {
                        counterpartyService.save(binder.getBean());
                        Runnable targetRefresh = refreshHolder[0] != null ? refreshHolder[0] : refresh;
                        targetRefresh.run();
                        d.close();
                    });
                    Button del = new Button("Удалить", e -> {
                        if (bean.getId() != null) {
                            counterpartyService.delete(bean);
                        }
                        Runnable targetRefresh = refreshHolder[0] != null ? refreshHolder[0] : refresh;
                        targetRefresh.run();
                        d.close();
                    });
                    del.addThemeVariants(ButtonVariant.LUMO_ERROR);
                    Button close = new Button("Закрыть", e -> d.close());
                    d.add(new FormLayout(name), new HorizontalLayout(save, del, close));
                    return d;
                },
                refresh -> refreshHolder[0] = refresh,
                context -> {
                    FileBuffer buffer = new FileBuffer();
                    Upload upload = new Upload(buffer);
                    upload.setAcceptedFileTypes(".xlsx");
                    upload.setMaxFiles(1);
                    upload.setMaxFileSize((int) IMPORT_MAX_FILE_SIZE);
                    upload.setDropAllowed(false);
                    upload.setAutoUpload(true);
                    upload.getStyle().set("padding", "0");
                    Button importButton = new Button("Импорт");
                    importButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                    upload.setUploadButton(importButton);

                    UploadI18N i18n = new UploadI18N();
                    i18n.setError(new UploadI18N.Error()
                            .setFileIsTooBig("Файл превышает 10 МБ")
                            .setIncorrectFileType("Неверный тип файла. Ожидается .xlsx")
                            .setTooManyFiles("Можно загрузить только один файл"));
                    upload.setI18n(i18n);

                    upload.addFileRejectedListener(event ->
                            showErrorNotification(event.getErrorMessage()));
                    upload.addFailedListener(event ->
                            showErrorNotification("Загрузка файла прервана"));
                    upload.addSucceededListener(event -> {
                        Path tempFile = buffer.getFileData().getFile().toPath();
                        try {
                            ExcelImportResult result = counterpartyService.importFromXlsx(tempFile);
                            showSuccessNotification(String.format(
                                    "Импорт контрагентов завершён: создано %d, обновлено %d, обработано %d",
                                    result.created(), result.updated(), result.processed()));
                            Runnable targetRefresh = refreshHolder[0] != null
                                    ? refreshHolder[0]
                                    : context.refresh();
                            if (targetRefresh != null) {
                                targetRefresh.run();
                            }
                        } catch (ExcelImportException ex) {
                            showErrorNotification(ex.getMessage());
                        } catch (Exception ex) {
                            showErrorNotification("Не удалось импортировать файл");
                        } finally {
                            try {
                                Files.deleteIfExists(tempFile);
                            } catch (IOException ignored) {
                            }
                            upload.clearFileList();
                        }
                    });

                    context.actions().setWidthFull();
                    upload.getStyle().set("margin-left", "auto");
                    context.actions().add(upload);
                });

        nameFilter.addValueChangeListener(e -> {
            if (refreshHolder[0] != null) {
                refreshHolder[0].run();
            }
        });

        return layout;
    }

    private VerticalLayout contractGrid() {
        Grid<Contract> grid = new Grid<>(Contract.class, false);

        TextField nameFilter = new TextField();
        nameFilter.setValueChangeMode(ValueChangeMode.EAGER);
        nameFilter.setClearButtonVisible(true);

        TextField internalNumberFilter = new TextField();
        internalNumberFilter.setValueChangeMode(ValueChangeMode.EAGER);
        internalNumberFilter.setClearButtonVisible(true);

        TextField externalNumberFilter = new TextField();
        externalNumberFilter.setValueChangeMode(ValueChangeMode.EAGER);
        externalNumberFilter.setClearButtonVisible(true);

        DatePicker fromDateFilter = new DatePicker();
        fromDateFilter.setPlaceholder("От");
        fromDateFilter.setClearButtonVisible(true);
        fromDateFilter.setWidthFull();
        fromDateFilter.getElement().getStyle().set("minWidth", "0");

        DatePicker toDateFilter = new DatePicker();
        toDateFilter.setPlaceholder("До");
        toDateFilter.setClearButtonVisible(true);
        toDateFilter.setWidthFull();
        toDateFilter.getElement().getStyle().set("minWidth", "0");

        HorizontalLayout dateFilters = new HorizontalLayout(fromDateFilter, toDateFilter);
        dateFilters.setWidthFull();
        dateFilters.setSpacing(false);
        dateFilters.setPadding(false);
        dateFilters.setAlignItems(Alignment.STRETCH);
        dateFilters.getStyle().set("gap", "var(--lumo-space-xs)");
        dateFilters.setFlexGrow(1, fromDateFilter, toDateFilter);

        TextField responsibleFilter = new TextField();
        responsibleFilter.setValueChangeMode(ValueChangeMode.EAGER);
        responsibleFilter.setClearButtonVisible(true);

        TextField counterpartyFilter = new TextField();
        counterpartyFilter.setValueChangeMode(ValueChangeMode.EAGER);
        counterpartyFilter.setClearButtonVisible(true);

        grid.addColumn(Contract::getName)
                .setHeader(columnHeaderWithFilter("Наименование", nameFilter));
        grid.addColumn(Contract::getInternalNumber)
                .setHeader(columnHeaderWithFilter("№ внутренний", internalNumberFilter));
        grid.addColumn(Contract::getExternalNumber)
                .setHeader(columnHeaderWithFilter("№ внешний", externalNumberFilter));
        grid.addColumn(item -> item.getContractDate() != null ? item.getContractDate().toString() : "—")
                .setHeader(columnHeaderWithFilter("Дата", dateFilters));
        grid.addColumn(Contract::getResponsible)
                .setHeader(columnHeaderWithFilter("Ответственный", responsibleFilter));
        grid.addColumn(item -> item.getCounterparty() != null ? item.getCounterparty().getLegalEntityName() : "—")
                .setHeader(columnHeaderWithFilter("Контрагент", counterpartyFilter));

        Supplier<List<Contract>> loader = () -> {
            String nameValue = normalizeFilterValue(nameFilter.getValue());
            String internalValue = normalizeFilterValue(internalNumberFilter.getValue());
            String externalValue = normalizeFilterValue(externalNumberFilter.getValue());
            String responsibleValue = normalizeFilterValue(responsibleFilter.getValue());
            String counterpartyValue = normalizeFilterValue(counterpartyFilter.getValue());
            LocalDate fromDate = fromDateFilter.getValue();
            LocalDate toDate = toDateFilter.getValue();

            return contractService.findAll().stream()
                    .filter(item -> matchesContractFilters(item, nameValue, internalValue, externalValue,
                            responsibleValue, fromDate, toDate))
                    .filter(item -> matchesFilter(item.getCounterparty() != null ?
                            item.getCounterparty().getLegalEntityName() : null, counterpartyValue))
                    .toList();
        };

        Runnable[] refreshHolder = new Runnable[1];

        VerticalLayout layout = genericGrid(Contract.class, grid,
                loader,
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
                    ComboBox<Counterparty> counterparty = new ComboBox<>("Контрагент");
                    List<Counterparty> counterpartyItems = new ArrayList<>(counterpartyService.findAll());
                    if (bean.getCounterparty() != null && bean.getCounterparty().getId() != null) {
                        boolean present = counterpartyItems.stream()
                                .anyMatch(item -> item.getId().equals(bean.getCounterparty().getId()));
                        if (!present) {
                            counterpartyItems.add(bean.getCounterparty());
                        }
                    }
                    counterparty.setItems(counterpartyItems);
                    counterparty.setItemLabelGenerator(Counterparty::getLegalEntityName);
                    counterparty.setClearButtonVisible(true);
                    counterparty.setWidthFull();

                    if (bean.getContractDate() != null) {
                        date.setValue(bean.getContractDate());
                    }

                    binder.forField(name).bind(Contract::getName, Contract::setName);
                    binder.bind(inum, Contract::getInternalNumber, Contract::setInternalNumber);
                    binder.bind(exnum, Contract::getExternalNumber, Contract::setExternalNumber);
                    binder.bind(date, Contract::getContractDate, Contract::setContractDate);
                    binder.bind(resp, Contract::getResponsible, Contract::setResponsible);
                    binder.bind(counterparty, Contract::getCounterparty, Contract::setCounterparty);
                    binder.setBean(bean);

                    Button save = new Button("Сохранить", e -> {
                        contractService.save(binder.getBean());
                        Runnable targetRefresh = refreshHolder[0] != null ? refreshHolder[0] : refresh;
                        targetRefresh.run();
                        d.close();
                    });
                    Button del = new Button("Удалить", e -> {
                        if (bean.getId() != null) {
                            contractService.delete(bean);
                        }
                        Runnable targetRefresh = refreshHolder[0] != null ? refreshHolder[0] : refresh;
                        targetRefresh.run();
                        d.close();
                    });
                    del.addThemeVariants(ButtonVariant.LUMO_ERROR);
                    Button close = new Button("Закрыть", e -> d.close());
                    d.add(new FormLayout(name, inum, exnum, date, resp, counterparty), new HorizontalLayout(save, del, close));
                    return d;
                },
                refresh -> refreshHolder[0] = refresh,
                context -> {
                    FileBuffer buffer = new FileBuffer();
                    Upload upload = new Upload(buffer);
                    upload.setAcceptedFileTypes(".xlsx");
                    upload.setMaxFiles(1);
                    upload.setMaxFileSize((int) IMPORT_MAX_FILE_SIZE);
                    upload.setDropAllowed(false);
                    upload.setAutoUpload(true);
                    upload.getStyle().set("padding", "0");
                    Button importButton = new Button("Импорт");
                    importButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                    upload.setUploadButton(importButton);

                    UploadI18N i18n = new UploadI18N();
                    i18n.setError(new UploadI18N.Error()
                            .setFileIsTooBig("Файл превышает 10 МБ")
                            .setIncorrectFileType("Неверный тип файла. Ожидается .xlsx")
                            .setTooManyFiles("Можно загрузить только один файл"));
                    upload.setI18n(i18n);

                    upload.addFileRejectedListener(event ->
                            showErrorNotification(event.getErrorMessage()));
                    upload.addFailedListener(event ->
                            showErrorNotification("Загрузка файла прервана"));
                    upload.addSucceededListener(event -> {
                        Path tempFile = buffer.getFileData().getFile().toPath();
                        try {
                            ExcelImportResult result = contractService.importFromXlsx(tempFile);
                            showSuccessNotification(String.format(
                                    "Импорт договоров завершён: создано %d, обновлено %d, обработано %d",
                                    result.created(), result.updated(), result.processed()));
                            Runnable targetRefresh = refreshHolder[0] != null
                                    ? refreshHolder[0]
                                    : context.refresh();
                            if (targetRefresh != null) {
                                targetRefresh.run();
                            }
                        } catch (ExcelImportException ex) {
                            showErrorNotification(ex.getMessage());
                        } catch (Exception ex) {
                            showErrorNotification("Не удалось импортировать файл");
                        } finally {
                            try {
                                Files.deleteIfExists(tempFile);
                            } catch (IOException ignored) {
                            }
                            upload.clearFileList();
                        }
                    });

                    context.actions().setWidthFull();
                    upload.getStyle().set("margin-left", "auto");
                    context.actions().add(upload);
                });

        nameFilter.addValueChangeListener(e -> {
            if (refreshHolder[0] != null) {
                refreshHolder[0].run();
            }
        });
        internalNumberFilter.addValueChangeListener(e -> {
            if (refreshHolder[0] != null) {
                refreshHolder[0].run();
            }
        });
        externalNumberFilter.addValueChangeListener(e -> {
            if (refreshHolder[0] != null) {
                refreshHolder[0].run();
            }
        });
        fromDateFilter.addValueChangeListener(e -> {
            if (refreshHolder[0] != null) {
                refreshHolder[0].run();
            }
        });
        toDateFilter.addValueChangeListener(e -> {
            if (refreshHolder[0] != null) {
                refreshHolder[0].run();
            }
        });
        responsibleFilter.addValueChangeListener(e -> {
            if (refreshHolder[0] != null) {
                refreshHolder[0].run();
            }
        });
        counterpartyFilter.addValueChangeListener(e -> {
            if (refreshHolder[0] != null) {
                refreshHolder[0].run();
            }
        });

        return layout;
    }

    private void showSuccessNotification(String message) {
        showNotification(message, NotificationVariant.LUMO_SUCCESS);
    }

    private void showErrorNotification(String message) {
        showNotification(message, NotificationVariant.LUMO_ERROR);
    }

    private void showNotification(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, 6000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(variant);
    }

    private record GenericGridContext(HorizontalLayout actions, Button createButton, Button deleteButton,
                                      Runnable refresh) {
    }
}
