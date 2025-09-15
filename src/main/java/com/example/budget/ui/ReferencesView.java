package com.example.budget.ui;

import com.example.budget.domain.*;
import com.example.budget.repo.*;
import com.example.budget.service.BdzService;
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
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.spring.annotation.UIScope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

@Component
@UIScope
public class ReferencesView extends SplitLayout {

    private final BdzService bdzService;
    private final BoRepository boRepository;
    private final ZgdRepository zgdRepository;
    private final CfoRepository cfoRepository;
    private final MvzRepository mvzRepository;
    private final ContractRepository contractRepository;

    private final ListBox<String> leftMenu = new ListBox<>();
    private final Div rightPanel = new Div();

    public ReferencesView(BdzService bdzService, BoRepository boRepository, ZgdRepository zgdRepository,
                          CfoRepository cfoRepository, MvzRepository mvzRepository,
                          ContractRepository contractRepository) {
        this.bdzService = bdzService;
        this.boRepository = boRepository;
        this.zgdRepository = zgdRepository;
        this.cfoRepository = cfoRepository;
        this.mvzRepository = mvzRepository;
        this.contractRepository = contractRepository;

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
        tree.addHierarchyColumn(Bdz::getName).setHeader("Наименование");
        tree.addColumn(Bdz::getCode).setHeader("Код");
        tree.setSelectionMode(Grid.SelectionMode.MULTI);
        tree.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        refreshBdz(tree);

        // open details on item click
        tree.addItemClickListener(ev -> openBdzCard(ev.getItem(), tree));

        create.addClickListener(e -> openBdzCard(new Bdz(), tree));
        delete.addClickListener(e -> {
            tree.getSelectedItems().forEach(item -> {
                if (item.getId() != null) bdzService.deleteById(item.getId());
            });
            refreshBdz(tree);
        });

        layout.add(new HorizontalLayout(create, delete), tree);
        layout.setFlexGrow(1, tree);
        return layout;
    }

    private void refreshBdz(TreeGrid<Bdz> tree) {
        java.util.List<Bdz> roots = bdzService.findRoots();
        tree.setItems(roots, b -> bdzService.findChildren(b.getId()));
    }

    private void openBdzCard(Bdz entity, TreeGrid<Bdz> tree) {
        if (entity.getId() != null) {
            entity = bdzService.findById(entity.getId());
        }

        Dialog dlg = new Dialog("Статья БДЗ");
        dlg.setWidth("500px");
        Binder<Bdz> binder = new Binder<>(Bdz.class);

        TextField code = new TextField("Код");
        TextField name = new TextField("Наименование");
        ComboBox<Bdz> parent = new ComboBox<>("Родитель");
        java.util.List<Bdz> options = bdzService.findAll().stream()
                .filter(b -> !Objects.equals(b.getId(), entity.getId()))
                .toList();
        parent.setItems(options);
        parent.setItemLabelGenerator(Bdz::getName);
        if (entity.getParent() != null) {
            Long pid = entity.getParent().getId();
            entity.setParent(options.stream()
                    .filter(b -> Objects.equals(b.getId(), pid))
                    .findFirst().orElse(null));
        }

        binder.bind(code, Bdz::getCode, Bdz::setCode);
        binder.bind(name, Bdz::getName, Bdz::setName);
        binder.bind(parent, Bdz::getParent, Bdz::setParent);
        binder.setBean(entity);

        Button save = new Button("Сохранить", e -> {
            bdzService.save(binder.getBean());
            refreshBdz(tree);
            dlg.close();
        });
        Button delete = new Button("Удалить", e -> {
            if (entity.getId() != null) {
                bdzService.deleteById(entity.getId());
                refreshBdz(tree);
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
                                           java.util.function.Supplier<List<T>> loader,
                                           java.util.function.Function<T, T> saver,
                                           java.util.function.Consumer<T> deleter,
                                           BiFunction<T, Runnable, Dialog> editorFactory) {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        Button create = new Button("Создать");
        Button delete = new Button("Удалить выбранные");
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);

        ListDataProvider<T> provider = new ListDataProvider<>(loader.get());
        grid.setItems(provider);
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        Runnable refresh = () -> {
            provider.getItems().clear();
            provider.getItems().addAll(loader.get());
            provider.refreshAll();
        };

        grid.addItemClickListener(e -> editorFactory.apply(e.getItem(), refresh).open());
        create.addClickListener(e -> editorFactory.apply(null, refresh).open());
        delete.addClickListener(e -> {
            grid.getSelectedItems().forEach(deleter);
            refresh.run();
        });

        layout.add(new HorizontalLayout(create, delete), grid);
        layout.setFlexGrow(1, grid);
        return layout;
    }

    private VerticalLayout boGrid() {
        Grid<Bo> grid = new Grid<>(Bo.class, false);
        grid.addColumn(Bo::getCode).setHeader("Код");
        grid.addColumn(Bo::getName).setHeader("Наименование");
        grid.addColumn(item -> item.getBdz() != null ? item.getBdz().getName() : "—").setHeader("БДЗ");

        return genericGrid(Bo.class, grid,
                () -> boRepository.findAll(),
                boRepository::save,
                boRepository::delete,
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
                    Button save = new Button("Сохранить", e -> { boRepository.save(binder.getBean()); refresh.run(); d.close(); });
                    Button del = new Button("Удалить", e -> { if (bean.getId()!=null) boRepository.delete(bean); refresh.run(); d.close(); });
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
                () -> zgdRepository.findAll(),
                zgdRepository::save,
                zgdRepository::delete,
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

                    Button save = new Button("Сохранить", e -> { zgdRepository.save(binder.getBean()); refresh.run(); d.close(); });
                    Button del = new Button("Удалить", e -> { if (bean.getId()!=null) zgdRepository.delete(bean); refresh.run(); d.close(); });
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
                () -> contractRepository.findAll(),
                contractRepository::save,
                contractRepository::delete,
                (selected, refresh) -> {
                    Contract bean = selected != null ? selected : new Contract();
                    Dialog d = new Dialog("Договор");
                    Binder<Contract> binder = new Binder<>(Contract.class);
                    TextField name = new TextField("Наименование");
                    TextField inum = new TextField("№ внутренний");
                    TextField exnum = new TextField("№ внешний");
                    TextField date = new TextField("Дата (YYYY-MM-DD)");
                    TextField resp = new TextField("Ответственный (ФИО)");

                    binder.forField(name).bind(Contract::getName, Contract::setName);
                    binder.bind(inum, Contract::getInternalNumber, Contract::setInternalNumber);
                    binder.bind(exnum, Contract::getExternalNumber, Contract::setExternalNumber);
                    binder.bind(resp, Contract::getResponsible, Contract::setResponsible);
                    binder.setBean(bean);

                    Button save = new Button("Сохранить", e -> {
                        try {
                            if (date.getValue() != null && !date.getValue().isBlank()) {
                                bean.setContractDate(java.time.LocalDate.parse(date.getValue()));
                            }
                            contractRepository.save(bean);
                            refresh.run();
                            d.close();
                        } catch (Exception ex) {
                            date.setInvalid(true);
                            date.setErrorMessage("Неверный формат даты");
                        }
                    });
                    Button del = new Button("Удалить", e -> { if (bean.getId()!=null) contractRepository.delete(bean); refresh.run(); d.close(); });
                    del.addThemeVariants(ButtonVariant.LUMO_ERROR);
                    Button close = new Button("Закрыть", e -> d.close());
                    d.add(new FormLayout(name, inum, exnum, date, resp), new HorizontalLayout(save, del, close));
                    return d;
                });
    }
}
