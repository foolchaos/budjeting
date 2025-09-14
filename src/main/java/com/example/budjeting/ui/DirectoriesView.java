package com.example.budjeting.ui;

import com.example.budjeting.entity.*;
import com.example.budjeting.repository.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.TreeGrid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.SplitLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.Route;

import java.util.List;
import java.util.Set;

@Route(value = "", layout = MainLayout.class)
public class DirectoriesView extends VerticalLayout {

    private final BudgetItemRepository budgetItemRepository;
    private final BoArticleRepository boArticleRepository;
    private final ZgdRepository zgdRepository;
    private final CfoRepository cfoRepository;
    private final MvzRepository mvzRepository;
    private final ContractRepository contractRepository;

    public DirectoriesView(BudgetItemRepository budgetItemRepository,
                           BoArticleRepository boArticleRepository,
                           ZgdRepository zgdRepository,
                           CfoRepository cfoRepository,
                           MvzRepository mvzRepository,
                           ContractRepository contractRepository) {
        this.budgetItemRepository = budgetItemRepository;
        this.boArticleRepository = boArticleRepository;
        this.zgdRepository = zgdRepository;
        this.cfoRepository = cfoRepository;
        this.mvzRepository = mvzRepository;
        this.contractRepository = contractRepository;

        setSizeFull();
        SplitLayout split = new SplitLayout();
        split.setSizeFull();

        Tabs tabs = new Tabs(
                new Tab("БДЗ"),
                new Tab("БО"),
                new Tab("ЗГД"),
                new Tab("ЦФО"),
                new Tab("МВЗ"),
                new Tab("Договор")
        );
        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        split.addToPrimary(tabs);

        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        split.addToSecondary(content);

        tabs.addSelectedChangeListener(e -> {
            content.removeAll();
            String label = e.getSelectedTab().getLabel();
            switch (label) {
                case "БДЗ" -> content.add(createBudgetItemTree());
                case "БО" -> content.add(createBoGrid());
                case "ЗГД" -> content.add(createZgdGrid());
                case "ЦФО" -> content.add(createCfoGrid());
                case "МВЗ" -> content.add(createMvzGrid());
                case "Договор" -> content.add(createContractGrid());
            }
        });

        add(split);
        tabs.setSelectedIndex(0);
    }

    private VerticalLayout createBudgetItemTree() {
        TreeGrid<BudgetItem> tree = new TreeGrid<>();
        tree.addHierarchyColumn(BudgetItem::getCode).setHeader("Код");
        tree.addColumn(BudgetItem::getName).setHeader("Наименование");
        refreshBudgetItemTree(tree);
        tree.setSelectionMode(Grid.SelectionMode.MULTI);

        Button add = new Button("Создать", e -> openBudgetItemDialog(new BudgetItem(), tree));
        Button edit = new Button("Редактировать", e -> tree.getSelectedItems().stream().findFirst().ifPresent(item -> openBudgetItemDialog(item, tree)));
        Button delete = new Button("Удалить", e -> {
            tree.getSelectedItems().forEach(budgetItemRepository::delete);
            refreshBudgetItemTree(tree);
        });
        return new VerticalLayout(new HorizontalLayout(add, edit, delete), tree);
    }

    private void refreshBudgetItemTree(TreeGrid<BudgetItem> tree) {
        List<BudgetItem> items = budgetItemRepository.findAll();
        tree.setItems(items, budgetItemRepository::findByParent);
    }

    private void openBudgetItemDialog(BudgetItem item, TreeGrid<BudgetItem> tree) {
        Dialog dialog = new Dialog();
        Binder<BudgetItem> binder = new Binder<>(BudgetItem.class);
        TextField code = new TextField("Код");
        TextField name = new TextField("Наименование");
        ComboBox<BudgetItem> parent = new ComboBox<>("Родитель");
        parent.setItems(budgetItemRepository.findAll());
        parent.setItemLabelGenerator(BudgetItem::getName);
        binder.bind(code, BudgetItem::getCode, BudgetItem::setCode);
        binder.bind(name, BudgetItem::getName, BudgetItem::setName);
        binder.bind(parent, BudgetItem::getParent, BudgetItem::setParent);
        binder.readBean(item);
        Button save = new Button("Сохранить", ev -> {
            try {
                binder.writeBean(item);
                budgetItemRepository.save(item);
                refreshBudgetItemTree(tree);
                dialog.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        dialog.add(new FormLayout(code, name, parent), save);
        dialog.open();
    }

    private VerticalLayout createBoGrid() {
        Grid<BoArticle> grid = new Grid<>(BoArticle.class, false);
        grid.addColumn(BoArticle::getCode).setHeader("Код");
        grid.addColumn(BoArticle::getName).setHeader("Наименование");
        ListDataProvider<BoArticle> provider = new ListDataProvider<>(boArticleRepository.findAll());
        grid.setDataProvider(provider);
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        Button add = new Button("Создать", e -> openBoDialog(new BoArticle(), provider));
        Button edit = new Button("Редактировать", e -> grid.getSelectedItems().stream().findFirst().ifPresent(item -> openBoDialog(item, provider)));
        Button delete = new Button("Удалить", e -> {
            Set<BoArticle> selected = grid.getSelectedItems();
            selected.forEach(boArticleRepository::delete);
            provider.getItems().clear();
            provider.getItems().addAll(boArticleRepository.findAll());
            provider.refreshAll();
        });
        return new VerticalLayout(new HorizontalLayout(add, edit, delete), grid);
    }

    private void openBoDialog(BoArticle item, ListDataProvider<BoArticle> provider) {
        Dialog dialog = new Dialog();
        Binder<BoArticle> binder = new Binder<>(BoArticle.class);
        TextField code = new TextField("Код");
        TextField name = new TextField("Наименование");
        ComboBox<BudgetItem> budgetItem = new ComboBox<>("Статья БДЗ");
        budgetItem.setItems(budgetItemRepository.findAll());
        budgetItem.setItemLabelGenerator(BudgetItem::getName);
        binder.bind(code, BoArticle::getCode, BoArticle::setCode);
        binder.bind(name, BoArticle::getName, BoArticle::setName);
        binder.bind(budgetItem, BoArticle::getBudgetItem, BoArticle::setBudgetItem);
        binder.readBean(item);
        Button save = new Button("Сохранить", ev -> {
            try {
                binder.writeBean(item);
                boArticleRepository.save(item);
                provider.getItems().clear();
                provider.getItems().addAll(boArticleRepository.findAll());
                provider.refreshAll();
                dialog.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        dialog.add(new FormLayout(code, name, budgetItem), save);
        dialog.open();
    }

    private VerticalLayout createZgdGrid() {
        Grid<Zgd> grid = new Grid<>(Zgd.class, false);
        grid.addColumn(Zgd::getFullName).setHeader("ФИО");
        grid.addColumn(Zgd::getDepartment).setHeader("Департамент");
        grid.addColumn(z -> z.getBudgetItem() != null ? z.getBudgetItem().getName() : "").setHeader("Статья БДЗ");
        ListDataProvider<Zgd> provider = new ListDataProvider<>(zgdRepository.findAll());
        grid.setDataProvider(provider);
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        Button add = new Button("Создать", e -> openZgdDialog(new Zgd(), provider));
        Button edit = new Button("Редактировать", e -> grid.getSelectedItems().stream().findFirst().ifPresent(item -> openZgdDialog(item, provider)));
        Button delete = new Button("Удалить", e -> {
            grid.getSelectedItems().forEach(zgdRepository::delete);
            provider.getItems().clear();
            provider.getItems().addAll(zgdRepository.findAll());
            provider.refreshAll();
        });
        return new VerticalLayout(new HorizontalLayout(add, edit, delete), grid);
    }

    private void openZgdDialog(Zgd item, ListDataProvider<Zgd> provider) {
        Dialog dialog = new Dialog();
        Binder<Zgd> binder = new Binder<>(Zgd.class);
        TextField fio = new TextField("ФИО");
        TextField dep = new TextField("Департамент");
        ComboBox<BudgetItem> budgetItem = new ComboBox<>("Статья БДЗ");
        budgetItem.setItems(budgetItemRepository.findAll());
        budgetItem.setItemLabelGenerator(BudgetItem::getName);
        binder.bind(fio, Zgd::getFullName, Zgd::setFullName);
        binder.bind(dep, Zgd::getDepartment, Zgd::setDepartment);
        binder.bind(budgetItem, Zgd::getBudgetItem, Zgd::setBudgetItem);
        binder.readBean(item);
        Button save = new Button("Сохранить", ev -> {
            try {
                binder.writeBean(item);
                zgdRepository.save(item);
                provider.getItems().clear();
                provider.getItems().addAll(zgdRepository.findAll());
                provider.refreshAll();
                dialog.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        dialog.add(new FormLayout(fio, dep, budgetItem), save);
        dialog.open();
    }

    private VerticalLayout createCfoGrid() {
        Grid<Cfo> grid = new Grid<>(Cfo.class, false);
        grid.addColumn(Cfo::getCode).setHeader("Код");
        grid.addColumn(Cfo::getName).setHeader("Наименование");
        ListDataProvider<Cfo> provider = new ListDataProvider<>(cfoRepository.findAll());
        grid.setDataProvider(provider);
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        Button add = new Button("Создать", e -> openCfoDialog(new Cfo(), provider));
        Button edit = new Button("Редактировать", e -> grid.getSelectedItems().stream().findFirst().ifPresent(item -> openCfoDialog(item, provider)));
        Button delete = new Button("Удалить", e -> {
            grid.getSelectedItems().forEach(cfoRepository::delete);
            provider.getItems().clear();
            provider.getItems().addAll(cfoRepository.findAll());
            provider.refreshAll();
        });
        return new VerticalLayout(new HorizontalLayout(add, edit, delete), grid);
    }

    private void openCfoDialog(Cfo item, ListDataProvider<Cfo> provider) {
        Dialog dialog = new Dialog();
        Binder<Cfo> binder = new Binder<>(Cfo.class);
        TextField code = new TextField("Код");
        TextField name = new TextField("Наименование");
        binder.bind(code, Cfo::getCode, Cfo::setCode);
        binder.bind(name, Cfo::getName, Cfo::setName);
        binder.readBean(item);
        Button save = new Button("Сохранить", ev -> {
            try {
                binder.writeBean(item);
                cfoRepository.save(item);
                provider.getItems().clear();
                provider.getItems().addAll(cfoRepository.findAll());
                provider.refreshAll();
                dialog.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        dialog.add(new FormLayout(code, name), save);
        dialog.open();
    }

    private VerticalLayout createMvzGrid() {
        Grid<Mvz> grid = new Grid<>(Mvz.class, false);
        grid.addColumn(Mvz::getCode).setHeader("Код");
        grid.addColumn(Mvz::getName).setHeader("Наименование");
        grid.addColumn(m -> m.getCfo() != null ? m.getCfo().getName() : "").setHeader("ЦФО");
        ListDataProvider<Mvz> provider = new ListDataProvider<>(mvzRepository.findAll());
        grid.setDataProvider(provider);
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        Button add = new Button("Создать", e -> openMvzDialog(new Mvz(), provider));
        Button edit = new Button("Редактировать", e -> grid.getSelectedItems().stream().findFirst().ifPresent(item -> openMvzDialog(item, provider)));
        Button delete = new Button("Удалить", e -> {
            grid.getSelectedItems().forEach(mvzRepository::delete);
            provider.getItems().clear();
            provider.getItems().addAll(mvzRepository.findAll());
            provider.refreshAll();
        });
        return new VerticalLayout(new HorizontalLayout(add, edit, delete), grid);
    }

    private void openMvzDialog(Mvz item, ListDataProvider<Mvz> provider) {
        Dialog dialog = new Dialog();
        Binder<Mvz> binder = new Binder<>(Mvz.class);
        TextField code = new TextField("Код");
        TextField name = new TextField("Наименование");
        ComboBox<Cfo> cfo = new ComboBox<>("ЦФО");
        cfo.setItems(cfoRepository.findAll());
        cfo.setItemLabelGenerator(Cfo::getName);
        binder.bind(code, Mvz::getCode, Mvz::setCode);
        binder.bind(name, Mvz::getName, Mvz::setName);
        binder.bind(cfo, Mvz::getCfo, Mvz::setCfo);
        binder.readBean(item);
        Button save = new Button("Сохранить", ev -> {
            try {
                binder.writeBean(item);
                mvzRepository.save(item);
                provider.getItems().clear();
                provider.getItems().addAll(mvzRepository.findAll());
                provider.refreshAll();
                dialog.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        dialog.add(new FormLayout(code, name, cfo), save);
        dialog.open();
    }

    private VerticalLayout createContractGrid() {
        Grid<Contract> grid = new Grid<>(Contract.class, false);
        grid.addColumn(Contract::getName).setHeader("Наименование");
        grid.addColumn(Contract::getInnerNumber).setHeader("Номер внутр.");
        grid.addColumn(Contract::getOuterNumber).setHeader("Номер внешн.");
        grid.addColumn(Contract::getContractDate).setHeader("Дата");
        grid.addColumn(Contract::getResponsible).setHeader("Ответственный");
        ListDataProvider<Contract> provider = new ListDataProvider<>(contractRepository.findAll());
        grid.setDataProvider(provider);
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        Button add = new Button("Создать", e -> openContractDialog(new Contract(), provider));
        Button edit = new Button("Редактировать", e -> grid.getSelectedItems().stream().findFirst().ifPresent(item -> openContractDialog(item, provider)));
        Button delete = new Button("Удалить", e -> {
            grid.getSelectedItems().forEach(contractRepository::delete);
            provider.getItems().clear();
            provider.getItems().addAll(contractRepository.findAll());
            provider.refreshAll();
        });
        return new VerticalLayout(new HorizontalLayout(add, edit, delete), grid);
    }

    private void openContractDialog(Contract item, ListDataProvider<Contract> provider) {
        Dialog dialog = new Dialog();
        Binder<Contract> binder = new Binder<>(Contract.class);
        TextField name = new TextField("Наименование");
        TextField inNum = new TextField("Номер внутр.");
        TextField outNum = new TextField("Номер внешн.");
        TextField date = new TextField("Дата договора");
        TextField resp = new TextField("Ответственный");
        binder.bind(name, Contract::getName, Contract::setName);
        binder.bind(inNum, Contract::getInnerNumber, Contract::setInnerNumber);
        binder.bind(outNum, Contract::getOuterNumber, Contract::setOuterNumber);
        binder.bind(date, c -> c.getContractDate() != null ? c.getContractDate().toString() : "", (c,v) -> c.setContractDate(java.time.LocalDate.parse(v)));
        binder.bind(resp, Contract::getResponsible, Contract::setResponsible);
        binder.readBean(item);
        Button save = new Button("Сохранить", ev -> {
            try {
                binder.writeBean(item);
                contractRepository.save(item);
                provider.getItems().clear();
                provider.getItems().addAll(contractRepository.findAll());
                provider.refreshAll();
                dialog.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        dialog.add(new FormLayout(name, inNum, outNum, date, resp), save);
        dialog.open();
    }
}
