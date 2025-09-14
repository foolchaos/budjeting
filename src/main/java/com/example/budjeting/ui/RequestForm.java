package com.example.budjeting.ui;

import com.example.budjeting.entity.*;
import com.example.budjeting.repository.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.data.converter.Result;
import com.vaadin.flow.data.converter.ValueContext;

import java.math.BigDecimal;
import java.util.function.Consumer;

public class RequestForm extends Dialog {
    private final Binder<Request> binder = new BeanValidationBinder<>(Request.class);
    private final ComboBox<Bdz> bdz = new ComboBox<>("Статья БДЗ");
    private final ComboBox<Bo> bo = new ComboBox<>("Статья БО");
    private final ComboBox<Cfo> cfo = new ComboBox<>("ЦФО");
    private final ComboBox<Mvz> mvz = new ComboBox<>("МВЗ");
    private final TextField vgo = new TextField("ВГО");
    private final NumberField amount = new NumberField("Сумма");
    private final NumberField amountWithoutVat = new NumberField("Сумма без НДС");
    private final TextField subject = new TextField("Предмет");
    private final TextField period = new TextField("Период");
    private final Checkbox introductory = new Checkbox("Вводный объект");
    private final TextField procurementMethod = new TextField("Способ закупки");

    private final TextField contractName = new TextField("Договор");
    private final TextField contractInternal = new TextField("Номер внутренний");
    private final TextField contractExternal = new TextField("Номер внешний");
    private final DatePicker contractDate = new DatePicker("Дата договора");
    private final TextField contractResponsible = new TextField("Ответственный");

    private Request request;

    public RequestForm(BdzRepository bdzRepository, BoRepository boRepository, CfoRepository cfoRepository, MvzRepository mvzRepository, Consumer<Request> onSave) {
        bdz.setItems(bdzRepository.findAll());
        bo.setItems(boRepository.findAll());
        cfo.setItems(cfoRepository.findAll());
        mvz.setItems(mvzRepository.findAll());

        bdz.addValueChangeListener(e -> bo.setItems(boRepository.findByBdz(e.getValue())));
        cfo.addValueChangeListener(e -> mvz.setItems(mvzRepository.findByCfo(e.getValue())));

        binder.bind(bdz, Request::getBdz, Request::setBdz);
        binder.bind(bo, Request::getBo, Request::setBo);
        binder.bind(cfo, Request::getCfo, Request::setCfo);
        binder.bind(mvz, Request::getMvz, Request::setMvz);
        binder.bind(vgo, Request::getVgo, Request::setVgo);
        binder.forField(amount).withConverter(new DoubleToBigDecimalConverter()).bind(Request::getAmount, Request::setAmount);
        binder.forField(amountWithoutVat).withConverter(new DoubleToBigDecimalConverter()).bind(Request::getAmountWithoutVat, Request::setAmountWithoutVat);
        binder.bind(subject, Request::getSubject, Request::setSubject);
        binder.bind(period, Request::getPeriod, Request::setPeriod);
        binder.bind(introductory, Request::isIntroductory, Request::setIntroductory);
        binder.bind(procurementMethod, Request::getProcurementMethod, Request::setProcurementMethod);

        binder.forField(contractName).bind(
                req -> req.getContract() != null ? req.getContract().getName() : "",
                (req, val) -> {
                    if (req.getContract() == null) req.setContract(new Contract());
                    req.getContract().setName(val);
                });
        binder.forField(contractInternal).bind(
                req -> req.getContract() != null ? req.getContract().getInternalNumber() : "",
                (req, val) -> {
                    if (req.getContract() == null) req.setContract(new Contract());
                    req.getContract().setInternalNumber(val);
                });
        binder.forField(contractExternal).bind(
                req -> req.getContract() != null ? req.getContract().getExternalNumber() : "",
                (req, val) -> {
                    if (req.getContract() == null) req.setContract(new Contract());
                    req.getContract().setExternalNumber(val);
                });
        binder.forField(contractDate).bind(
                req -> req.getContract() != null ? req.getContract().getContractDate() : null,
                (req, val) -> {
                    if (req.getContract() == null) req.setContract(new Contract());
                    req.getContract().setContractDate(val);
                });
        binder.forField(contractResponsible).bind(
                req -> req.getContract() != null ? req.getContract().getResponsible() : "",
                (req, val) -> {
                    if (req.getContract() == null) req.setContract(new Contract());
                    req.getContract().setResponsible(val);
                });

        FormLayout layout = new FormLayout(bdz, bo, cfo, mvz, vgo, amount, amountWithoutVat, subject, period, introductory, procurementMethod,
                contractName, contractInternal, contractExternal, contractDate, contractResponsible);
        add(layout);

        Button save = new Button("Сохранить", e -> {
            if (binder.writeBeanIfValid(request)) {
                onSave.accept(request);
                close();
            }
        });
        Button cancel = new Button("Отмена", e -> close());
        add(new HorizontalLayout(save, cancel));
    }

    public void setRequest(Request request) {
        this.request = request;
        binder.readBean(request);
    }

    private static class DoubleToBigDecimalConverter implements Converter<Double, BigDecimal> {
        @Override
        public Result<BigDecimal> convertToModel(Double value, ValueContext context) {
            return value == null ? Result.ok(null) : Result.ok(BigDecimal.valueOf(value));
        }

        @Override
        public Double convertToPresentation(BigDecimal value, ValueContext context) {
            return value == null ? null : value.doubleValue();
        }
    }
}
