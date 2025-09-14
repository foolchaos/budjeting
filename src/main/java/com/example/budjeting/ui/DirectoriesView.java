package com.example.budjeting.ui;

import com.example.budjeting.entity.*;
import com.example.budjeting.repository.*;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.crud.BinderCrudEditor;
import com.vaadin.flow.component.crud.Crud;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
public class DirectoriesView extends SplitLayout {

    private final BudgetItemRepository budgetItemRepository;
    private final BoArticleRepository boArticleRepository;
    private final ZgdRepository zgdRepository;
    private final CfoRepository cfoRepository;
    private final MvzRepository mvzRepository;
    private final ContractRepository contractRepository;

    private final ListBox<String> list = new ListBox<>();
    private final Div content = new Div();

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
        list.setItems("БДЗ", "БО", "ЗГД", "ЦФО", "МВЗ", "Договор");
        list.addValueChangeListener(e -> show(e.getValue()));
        addToPrimary(list);
        content.setSizeFull();
        addToSecondary(content);
        setSplitterPosition(20);
        list.setValue("БДЗ");
    }

    private void show(String value) {
        content.removeAll();
        Component comp;
        switch (value) {
            case "БО" -> comp = createBoCrud();
            case "ЗГД" -> comp = createZgdCrud();
            case "ЦФО" -> comp = createCfoCrud();
            case "МВЗ" -> comp = createMvzCrud();
            case "Договор" -> comp = createContractCrud();
            default -> comp = createBudgetItemCrud();
        }
        content.add(comp);
    }

    private Component createBudgetItemCrud() {
        var grid = new com.vaadin.flow.component.treegrid.TreeGrid<BudgetItem>();
        grid.addHierarchyColumn(BudgetItem::getName).setHeader("Наименование");
        grid.addColumn(BudgetItem::getCode).setHeader("Код");
        grid.setItems(budgetItemRepository.findByParentIsNull(), budgetItemRepository::findByParent);

        TextField code = new TextField("Код");
        TextField name = new TextField("Наименование");
        ComboBox<BudgetItem> parent = new ComboBox<>("Родитель");
        parent.setItems(budgetItemRepository.findAll());
        parent.setItemLabelGenerator(BudgetItem::getName);
        FormLayout form = new FormLayout(code, name, parent);
        BeanValidationBinder<BudgetItem> binder = new BeanValidationBinder<>(BudgetItem.class);
        binder.bind(code, "code");
        binder.bind(name, "name");
        binder.bind(parent, "parent");
        Crud<BudgetItem> crud = new Crud<>(BudgetItem.class, grid, new BinderCrudEditor<>(binder, form));
        crud.setOperations(
                () -> budgetItemRepository.findAll(),
                budgetItemRepository::save,
                budgetItemRepository::save,
                budgetItemRepository::delete
        );
        crud.addSaveListener(e -> grid.setItems(budgetItemRepository.findByParentIsNull(), budgetItemRepository::findByParent));
        crud.addDeleteListener(e -> grid.setItems(budgetItemRepository.findByParentIsNull(), budgetItemRepository::findByParent));
        return crud;
    }

    private Component createBoCrud() {
        Grid<BoArticle> grid = new Grid<>(BoArticle.class, false);
        grid.addColumn(BoArticle::getCode).setHeader("Код");
        grid.addColumn(BoArticle::getName).setHeader("Наименование");
        grid.addColumn(a -> a.getBudgetItem() != null ? a.getBudgetItem().getName() : "").setHeader("Статья БДЗ");

        TextField code = new TextField("Код");
        TextField name = new TextField("Наименование");
        ComboBox<BudgetItem> budgetItem = new ComboBox<>("Статья БДЗ");
        budgetItem.setItems(budgetItemRepository.findAll());
        budgetItem.setItemLabelGenerator(BudgetItem::getName);
        FormLayout form = new FormLayout(code, name, budgetItem);
        BeanValidationBinder<BoArticle> binder = new BeanValidationBinder<>(BoArticle.class);
        binder.bind(code, "code");
        binder.bind(name, "name");
        binder.bind(budgetItem, "budgetItem");
        Crud<BoArticle> crud = new Crud<>(BoArticle.class, grid, new BinderCrudEditor<>(binder, form));
        crud.setOperations(
                () -> boArticleRepository.findAll(),
                boArticleRepository::save,
                boArticleRepository::save,
                boArticleRepository::delete
        );
        return crud;
    }

    private Component createZgdCrud() {
        Grid<Zgd> grid = new Grid<>(Zgd.class, false);
        grid.addColumn(Zgd::getFio).setHeader("ФИО");
        grid.addColumn(Zgd::getDepartment).setHeader("Департамент");
        grid.addColumn(z -> z.getBudgetItem() != null ? z.getBudgetItem().getName() : "").setHeader("Статья БДЗ");

        TextField fio = new TextField("ФИО");
        TextField department = new TextField("Департамент");
        ComboBox<BudgetItem> budgetItem = new ComboBox<>("Статья БДЗ");
        budgetItem.setItems(budgetItemRepository.findAll());
        budgetItem.setItemLabelGenerator(BudgetItem::getName);
        FormLayout form = new FormLayout(fio, department, budgetItem);
        BeanValidationBinder<Zgd> binder = new BeanValidationBinder<>(Zgd.class);
        binder.bind(fio, "fio");
        binder.bind(department, "department");
        binder.bind(budgetItem, "budgetItem");
        Crud<Zgd> crud = new Crud<>(Zgd.class, grid, new BinderCrudEditor<>(binder, form));
        crud.setOperations(
                () -> zgdRepository.findAll(),
                zgdRepository::save,
                zgdRepository::save,
                zgdRepository::delete
        );
        return crud;
    }

    private Component createCfoCrud() {
        Grid<Cfo> grid = new Grid<>(Cfo.class, false);
        grid.addColumn(Cfo::getCode).setHeader("Код");
        grid.addColumn(Cfo::getName).setHeader("Наименование");

        TextField code = new TextField("Код");
        TextField name = new TextField("Наименование");
        FormLayout form = new FormLayout(code, name);
        BeanValidationBinder<Cfo> binder = new BeanValidationBinder<>(Cfo.class);
        binder.bind(code, "code");
        binder.bind(name, "name");
        Crud<Cfo> crud = new Crud<>(Cfo.class, grid, new BinderCrudEditor<>(binder, form));
        crud.setOperations(
                () -> cfoRepository.findAll(),
                cfoRepository::save,
                cfoRepository::save,
                cfoRepository::delete
        );
        return crud;
    }

    private Component createMvzCrud() {
        Grid<Mvz> grid = new Grid<>(Mvz.class, false);
        grid.addColumn(Mvz::getCode).setHeader("Код");
        grid.addColumn(Mvz::getName).setHeader("Наименование");
        grid.addColumn(m -> m.getCfo() != null ? m.getCfo().getName() : "").setHeader("ЦФО");

        TextField code = new TextField("Код");
        TextField name = new TextField("Наименование");
        ComboBox<Cfo> cfo = new ComboBox<>("ЦФО");
        cfo.setItems(cfoRepository.findAll());
        cfo.setItemLabelGenerator(Cfo::getName);
        FormLayout form = new FormLayout(code, name, cfo);
        BeanValidationBinder<Mvz> binder = new BeanValidationBinder<>(Mvz.class);
        binder.bind(code, "code");
        binder.bind(name, "name");
        binder.bind(cfo, "cfo");
        Crud<Mvz> crud = new Crud<>(Mvz.class, grid, new BinderCrudEditor<>(binder, form));
        crud.setOperations(
                () -> mvzRepository.findAll(),
                mvzRepository::save,
                mvzRepository::save,
                mvzRepository::delete
        );
        return crud;
    }

    private Component createContractCrud() {
        Grid<Contract> grid = new Grid<>(Contract.class, false);
        grid.addColumn(Contract::getName).setHeader("Наименование");
        grid.addColumn(Contract::getInternalNumber).setHeader("Номер внутр.");
        grid.addColumn(Contract::getExternalNumber).setHeader("Номер внешн.");
        grid.addColumn(Contract::getDate).setHeader("Дата");
        grid.addColumn(Contract::getResponsible).setHeader("Ответственный");

        TextField name = new TextField("Наименование");
        TextField internal = new TextField("Номер внутр.");
        TextField external = new TextField("Номер внешн.");
        DatePicker date = new DatePicker("Дата");
        TextField responsible = new TextField("Ответственный");
        FormLayout form = new FormLayout(name, internal, external, date, responsible);
        BeanValidationBinder<Contract> binder = new BeanValidationBinder<>(Contract.class);
        binder.bind(name, "name");
        binder.bind(internal, "internalNumber");
        binder.bind(external, "externalNumber");
        binder.bind(date, "date");
        binder.bind(responsible, "responsible");
        Crud<Contract> crud = new Crud<>(Contract.class, grid, new BinderCrudEditor<>(binder, form));
        crud.setOperations(
                () -> contractRepository.findAll(),
                contractRepository::save,
                contractRepository::save,
                contractRepository::delete
        );
        return crud;
    }
}
