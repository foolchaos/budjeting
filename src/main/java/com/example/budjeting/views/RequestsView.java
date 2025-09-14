package com.example.budjeting.views;

import com.example.budjeting.entity.*;
import com.example.budjeting.repository.*;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.crud.BinderCrudEditor;
import com.vaadin.flow.component.crud.Crud;
import com.vaadin.flow.component.crud.CrudEditor;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.PropertyId;
import com.vaadin.flow.data.converter.StringToBigDecimalConverter;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "requests", layout = MainLayout.class)
@PageTitle("Заявки")
public class RequestsView extends com.vaadin.flow.component.orderedlayout.VerticalLayout {

    public RequestsView(RequestRepository requestRepo,
                        BdzArticleRepository bdzRepo,
                        BoArticleRepository boRepo,
                        CfoRepository cfoRepo,
                        MvzRepository mvzRepo,
                        CuratorRepository curatorRepo) {
        setSizeFull();

        Crud<Request> crud = createCrud(requestRepo, bdzRepo, boRepo, cfoRepo, mvzRepo);
        add(crud);
    }

    private Crud<Request> createCrud(RequestRepository requestRepo,
                                     BdzArticleRepository bdzRepo,
                                     BoArticleRepository boRepo,
                                     CfoRepository cfoRepo,
                                     MvzRepository mvzRepo) {
        ComboBox<BdzArticle> bdz = new ComboBox<>("Статья БДЗ");
        bdz.setItems(bdzRepo.findAll());
        bdz.setItemLabelGenerator(BdzArticle::getName);

        ComboBox<BoArticle> bo = new ComboBox<>("Статья БО");
        bo.setItemLabelGenerator(BoArticle::getName);
        bdz.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                bo.setItems(boRepo.findAll().stream()
                        .filter(b -> e.getValue().equals(b.getBdzArticle()))
                        .toList());
            } else {
                bo.clear();
                bo.setItems();
            }
        });

        ComboBox<Cfo> cfo = new ComboBox<>("ЦФО");
        cfo.setItems(cfoRepo.findAll());
        cfo.setItemLabelGenerator(Cfo::getName);

        ComboBox<Mvz> mvz = new ComboBox<>("МВЗ");
        mvz.setItemLabelGenerator(Mvz::getName);
        cfo.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                mvz.setItems(mvzRepo.findAll().stream()
                        .filter(m -> e.getValue().equals(m.getCfo()))
                        .toList());
            } else {
                mvz.clear();
                mvz.setItems();
            }
        });

        TextField vgo = new TextField("ВГО");
        TextField amount = new TextField("Сумма");
        TextField amountNoVat = new TextField("Сумма без НДС");
        TextField subject = new TextField("Предмет договора");
        TextField period = new TextField("Период");
        Checkbox intro = new Checkbox("Вводный объект");
        TextField method = new TextField("Способ закупки");
        TextField curator = new TextField("Курирующий ЗГД");
        curator.setReadOnly(true);
        bdz.addValueChangeListener(e -> {
            if (e.getValue() != null && e.getValue().getCurator() != null) {
                curator.setValue(e.getValue().getCurator().getFullName());
            } else {
                curator.clear();
            }
        });

        FormLayout form = new FormLayout(bdz, bo, cfo, mvz, vgo, amount, amountNoVat, subject, period, intro, method, curator);
        BeanValidationBinder<Request> binder = new BeanValidationBinder<>(Request.class);
        binder.bind(bdz, "bdzArticle");
        binder.bind(bo, "boArticle");
        binder.bind(cfo, "cfo");
        binder.bind(mvz, "mvz");
        binder.bind(vgo, "vgo");
        binder.forField(amount).withConverter(new StringToBigDecimalConverter(""))
                .bind("amount");
        binder.forField(amountNoVat).withConverter(new StringToBigDecimalConverter(""))
                .bind("amountWithoutVat");
        binder.bind(subject, "contractSubject");
        binder.bind(period, "period");
        binder.bind(intro, "introductoryObject");
        binder.bind(method, "procurementMethod");
        // curator is set automatically

        CrudEditor<Request> editor = new BinderCrudEditor<>(binder, form);
        Crud<Request> crud = new Crud<>(Request.class, editor);
        crud.setDataProvider(DataProvider.ofCollection(requestRepo.findAll()));
        crud.addSaveListener(e -> {
            Request r = e.getItem();
            if (r.getBdzArticle() != null && r.getBdzArticle().getCurator() != null) {
                r.setCurator(r.getBdzArticle().getCurator());
            }
            requestRepo.save(r);
            crud.setDataProvider(DataProvider.ofCollection(requestRepo.findAll()));
        });
        crud.addDeleteListener(e -> {
            requestRepo.delete(e.getItem());
            crud.setDataProvider(DataProvider.ofCollection(requestRepo.findAll()));
        });
        return crud;
    }
}
