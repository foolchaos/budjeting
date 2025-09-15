package com.example.budget.ui;

import com.example.budget.domain.*;
import com.example.budget.repo.*;
import com.example.budget.service.BdzService;
import com.example.budget.service.BoService;
import com.example.budget.service.ContractService;
import com.example.budget.service.ZgdService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.spring.annotation.UIScope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
        tree.addHierarchyColumn(Bdz::getCode).setHeader("Код");
        tree.addColumn(Bdz::getName).setHeader("Наименование");
        tree.setSelectionMode(Grid.SelectionMode.MULTI);
        tree.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        tree.setHeightFull();

        Select<Integer> pageSizeSelect = new Select<>();
        pageSizeSelect.setLabel("Строк на странице");
        pageSizeSelect.setItems(5, 10, 25, 50);
        pageSizeSelect.setValue(10);

        Button prev = new Button("Назад");
        Button next = new Button("Вперёд");
        Span pageInfo = new Span();

        List<Bdz> roots = new ArrayList<>();
        final int[] currentPage = {0};

        Runnable updatePage = () -> {
            int pageSize = pageSizeSelect.getValue();
            int total = roots.size();

            if (total == 0) {
                tree.setItems(Collections.emptyList(), b -> bdzService.findChildren(b.getId()));
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
            tree.setItems(roots.subList(from, to), b -> bdzService.findChildren(b.getId()));
            pageInfo.setText(String.format("%d–%d из %d", from + 1, to, total));

            prev.setEnabled(currentPage[0] > 0);
            next.setEnabled(currentPage[0] < pageCount - 1);
        };

        Runnable reload = () -> {
            roots.clear();
            roots.addAll(bdzService.findRoots());
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
        grid.addColumn(Bo::getCode).setHeader("Код");
        grid.addColumn(Bo::getName).setHeader("Наименование");
        grid.addColumn(item -> item.getBdz() != null ? item.getBdz().getName() : "—").setHeader("БДЗ");

        return genericGrid(Bo.class, grid,
                boService::findAll,
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
                    bdz.setItemLabelGenerator(Bdz::getName);
                    binder.bind(code, Bo::getCode, Bo::setCode);
                    binder.bind(name, Bo::getName, Bo::setName);
                    binder.bind(bdz, Bo::getBdz, Bo::setBdz);
                    binder.setBean(bean);
                    Button save = new Button("Сохранить", e -> { boService.save(binder.getBean()); refresh.run(); d.close(); });
                    Button del = new Button("Удалить", e -> { if (bean.getId()!=null) boService.delete(bean); refresh.run(); d.close(); });
                    del.addThemeVariants(ButtonVariant.LUMO_ERROR);
                    Button close = new Button("Закрыть", e -> d.close());
                    d.add(new FormLayout(code, name, bdz), new HorizontalLayout(save, del, close));
                    return d;
                });
    }

    private VerticalLayout zgdGrid() {
        Grid<Zgd> grid = new Grid<>(Zgd.class, false);
        grid.addColumn(Zgd::getFullName).setHeader("ФИО");
        grid.addColumn(Zgd::getDepartment).setHeader("Департамент");
        grid.addColumn(item -> item.getBdz() != null ? item.getBdz().getName() : "—").setHeader("БДЗ");

        return genericGrid(Zgd.class, grid,
                zgdService::findAll,
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
                    bdz.setItemLabelGenerator(Bdz::getName);

                    binder.bind(fio, Zgd::getFullName, Zgd::setFullName);
                    binder.bind(dep, Zgd::getDepartment, Zgd::setDepartment);
                    binder.bind(bdz, Zgd::getBdz, Zgd::setBdz);
                    binder.setBean(bean);

                    Button save = new Button("Сохранить", e -> { zgdService.save(binder.getBean()); refresh.run(); d.close(); });
                    Button del = new Button("Удалить", e -> { if (bean.getId()!=null) zgdService.delete(bean); refresh.run(); d.close(); });
                    del.addThemeVariants(ButtonVariant.LUMO_ERROR);
                    Button close = new Button("Закрыть", e -> d.close());
                    d.add(new FormLayout(fio, dep, bdz), new HorizontalLayout(save, del, close));
                    return d;
                });
    }

    private VerticalLayout cfoGrid() {
        Grid<Cfo> grid = new Grid<>(Cfo.class, false);
        grid.addColumn(Cfo::getCode).setHeader("Код");
        grid.addColumn(Cfo::getName).setHeader("Наименование");

        return genericGrid(Cfo.class, grid,
                () -> cfoRepository.findAll(),
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
                    Button save = new Button("Сохранить", e -> { cfoRepository.save(binder.getBean()); refresh.run(); d.close(); });
                    Button del = new Button("Удалить", e -> { if (bean.getId()!=null) cfoRepository.delete(bean); refresh.run(); d.close(); });
                    del.addThemeVariants(ButtonVariant.LUMO_ERROR);
                    Button close = new Button("Закрыть", e -> d.close());
                    d.add(new FormLayout(code, name), new HorizontalLayout(save, del, close));
                    return d;
                });
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
