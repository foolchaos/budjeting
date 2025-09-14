package com.example.budjeting.views;

import com.example.budjeting.entity.*;
import com.example.budjeting.repository.*;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.crud.BinderCrudEditor;
import com.vaadin.flow.component.crud.Crud;
import com.vaadin.flow.component.crud.CrudEditor;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Справочники")
public class CatalogsView extends SplitLayout {

    public CatalogsView(BdzArticleRepository bdzRepo,
                        BoArticleRepository boRepo,
                        CuratorRepository curatorRepo,
                        CfoRepository cfoRepo,
                        MvzRepository mvzRepo,
                        ContractRepository contractRepo) {
        setSizeFull();
        Tabs tabs = new Tabs();
        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        Tab bdzTab = new Tab("БДЗ");
        Tab boTab = new Tab("БО");
        Tab curatorTab = new Tab("ЗГД");
        Tab cfoTab = new Tab("ЦФО");
        Tab mvzTab = new Tab("МВЗ");
        Tab contractTab = new Tab("Договор");
        tabs.add(bdzTab, boTab, curatorTab, cfoTab, mvzTab, contractTab);
        addToPrimary(tabs);

        com.vaadin.flow.component.orderedlayout.VerticalLayout content = new com.vaadin.flow.component.orderedlayout.VerticalLayout();
        content.setSizeFull();
        addToSecondary(content);

        tabs.addSelectedChangeListener(e -> {
            content.removeAll();
            Tab selected = e.getSelectedTab();
            if (selected == bdzTab) {
                content.add(createBdzCrud(bdzRepo));
            } else if (selected == boTab) {
                content.add(createBoCrud(boRepo, bdzRepo));
            } else if (selected == curatorTab) {
                content.add(createCuratorCrud(curatorRepo, bdzRepo));
            } else if (selected == cfoTab) {
                content.add(createCfoCrud(cfoRepo));
            } else if (selected == mvzTab) {
                content.add(createMvzCrud(mvzRepo, cfoRepo));
            } else if (selected == contractTab) {
                content.add(createContractCrud(contractRepo));
            }
        });

        tabs.setSelectedTab(bdzTab);
    }

    private Component createBdzCrud(BdzArticleRepository repo) {
        TextField code = new TextField("Код");
        TextField name = new TextField("Наименование");
        ComboBox<BdzArticle> parent = new ComboBox<>("Родитель");
        parent.setItems(repo.findAll());
        parent.setItemLabelGenerator(BdzArticle::getName);

        FormLayout form = new FormLayout(code, name, parent);
        BeanValidationBinder<BdzArticle> binder = new BeanValidationBinder<>(BdzArticle.class);
        binder.bind(code, "code");
        binder.bind(name, "name");
        binder.bind(parent, "parent");
        CrudEditor<BdzArticle> editor = new BinderCrudEditor<>(binder, form);

        Crud<BdzArticle> crud = new Crud<>(BdzArticle.class, editor);
        crud.setDataProvider(DataProvider.ofCollection(repo.findAll()));
        crud.addSaveListener(e -> {
            repo.save(e.getItem());
            parent.setItems(repo.findAll());
            crud.setDataProvider(DataProvider.ofCollection(repo.findAll()));
        });
        crud.addDeleteListener(e -> {
            repo.delete(e.getItem());
            parent.setItems(repo.findAll());
            crud.setDataProvider(DataProvider.ofCollection(repo.findAll()));
        });
        return crud;
    }

    private Component createBoCrud(BoArticleRepository repo, BdzArticleRepository bdzRepo) {
        TextField code = new TextField("Код");
        TextField name = new TextField("Наименование");
        ComboBox<BdzArticle> bdz = new ComboBox<>("Статья БДЗ");
        bdz.setItems(bdzRepo.findAll());
        bdz.setItemLabelGenerator(BdzArticle::getName);
        FormLayout form = new FormLayout(code, name, bdz);
        BeanValidationBinder<BoArticle> binder = new BeanValidationBinder<>(BoArticle.class);
        binder.bind(code, "code");
        binder.bind(name, "name");
        binder.bind(bdz, "bdzArticle");
        CrudEditor<BoArticle> editor = new BinderCrudEditor<>(binder, form);
        Crud<BoArticle> crud = new Crud<>(BoArticle.class, editor);
        crud.setDataProvider(DataProvider.ofCollection(repo.findAll()));
        crud.addSaveListener(e -> {
            repo.save(e.getItem());
            crud.setDataProvider(DataProvider.ofCollection(repo.findAll()));
        });
        crud.addDeleteListener(e -> {
            repo.delete(e.getItem());
            crud.setDataProvider(DataProvider.ofCollection(repo.findAll()));
        });
        return crud;
    }

    private Component createCuratorCrud(CuratorRepository repo, BdzArticleRepository bdzRepo) {
        TextField fullName = new TextField("ФИО");
        TextField department = new TextField("Департамент");
        ComboBox<BdzArticle> bdz = new ComboBox<>("Статья БДЗ");
        bdz.setItems(bdzRepo.findAll());
        bdz.setItemLabelGenerator(BdzArticle::getName);
        FormLayout form = new FormLayout(fullName, department, bdz);
        BeanValidationBinder<Curator> binder = new BeanValidationBinder<>(Curator.class);
        binder.bind(fullName, "fullName");
        binder.bind(department, "department");
        binder.bind(bdz, "bdzArticle");
        CrudEditor<Curator> editor = new BinderCrudEditor<>(binder, form);
        Crud<Curator> crud = new Crud<>(Curator.class, editor);
        crud.setDataProvider(DataProvider.ofCollection(repo.findAll()));
        crud.addSaveListener(e -> {
            repo.save(e.getItem());
            crud.setDataProvider(DataProvider.ofCollection(repo.findAll()));
        });
        crud.addDeleteListener(e -> {
            repo.delete(e.getItem());
            crud.setDataProvider(DataProvider.ofCollection(repo.findAll()));
        });
        return crud;
    }

    private Component createCfoCrud(CfoRepository repo) {
        TextField code = new TextField("Код");
        TextField name = new TextField("Наименование");
        FormLayout form = new FormLayout(code, name);
        BeanValidationBinder<Cfo> binder = new BeanValidationBinder<>(Cfo.class);
        binder.bind(code, "code");
        binder.bind(name, "name");
        CrudEditor<Cfo> editor = new BinderCrudEditor<>(binder, form);
        Crud<Cfo> crud = new Crud<>(Cfo.class, editor);
        crud.setDataProvider(DataProvider.ofCollection(repo.findAll()));
        crud.addSaveListener(e -> {
            repo.save(e.getItem());
            crud.setDataProvider(DataProvider.ofCollection(repo.findAll()));
        });
        crud.addDeleteListener(e -> {
            repo.delete(e.getItem());
            crud.setDataProvider(DataProvider.ofCollection(repo.findAll()));
        });
        return crud;
    }

    private Component createMvzCrud(MvzRepository repo, CfoRepository cfoRepo) {
        TextField code = new TextField("Код");
        TextField name = new TextField("Наименование");
        ComboBox<Cfo> cfo = new ComboBox<>("ЦФО");
        cfo.setItems(cfoRepo.findAll());
        cfo.setItemLabelGenerator(Cfo::getName);
        FormLayout form = new FormLayout(code, name, cfo);
        BeanValidationBinder<Mvz> binder = new BeanValidationBinder<>(Mvz.class);
        binder.bind(code, "code");
        binder.bind(name, "name");
        binder.bind(cfo, "cfo");
        CrudEditor<Mvz> editor = new BinderCrudEditor<>(binder, form);
        Crud<Mvz> crud = new Crud<>(Mvz.class, editor);
        crud.setDataProvider(DataProvider.ofCollection(repo.findAll()));
        crud.addSaveListener(e -> {
            repo.save(e.getItem());
            crud.setDataProvider(DataProvider.ofCollection(repo.findAll()));
        });
        crud.addDeleteListener(e -> {
            repo.delete(e.getItem());
            crud.setDataProvider(DataProvider.ofCollection(repo.findAll()));
        });
        return crud;
    }

    private Component createContractCrud(ContractRepository repo) {
        TextField name = new TextField("Наименование");
        TextField internal = new TextField("Номер внутренний");
        TextField external = new TextField("Номер внешний");
        TextField date = new TextField("Дата договора");
        TextField responsible = new TextField("Ответственный");
        FormLayout form = new FormLayout(name, internal, external, date, responsible);
        BeanValidationBinder<Contract> binder = new BeanValidationBinder<>(Contract.class);
        binder.bind(name, "name");
        binder.bind(internal, "internalNumber");
        binder.bind(external, "externalNumber");
        binder.bind(responsible, "responsible");
        // date binding left simple as string
        CrudEditor<Contract> editor = new BinderCrudEditor<>(binder, form);
        Crud<Contract> crud = new Crud<>(Contract.class, editor);
        crud.setDataProvider(DataProvider.ofCollection(repo.findAll()));
        crud.addSaveListener(e -> {
            repo.save(e.getItem());
            crud.setDataProvider(DataProvider.ofCollection(repo.findAll()));
        });
        crud.addDeleteListener(e -> {
            repo.delete(e.getItem());
            crud.setDataProvider(DataProvider.ofCollection(repo.findAll()));
        });
        return crud;
    }
}
