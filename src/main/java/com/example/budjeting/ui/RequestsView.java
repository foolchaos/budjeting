package com.example.budjeting.ui;

import com.example.budjeting.entity.*;
import com.example.budjeting.repository.*;
import com.example.budjeting.service.RequestService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.Route;

import java.util.stream.Collectors;

@Route(value = "requests", layout = MainLayout.class)
public class RequestsView extends VerticalLayout {

    private final RequestRepository requestRepository;
    private final RequestService requestService;
    private final BudgetItemRepository budgetItemRepository;
    private final BoArticleRepository boArticleRepository;
    private final CfoRepository cfoRepository;
    private final MvzRepository mvzRepository;
    private final ContractRepository contractRepository;

    private final Grid<Request> grid = new Grid<>(Request.class, false);

    public RequestsView(RequestRepository requestRepository,
                        RequestService requestService,
                        BudgetItemRepository budgetItemRepository,
                        BoArticleRepository boArticleRepository,
                        CfoRepository cfoRepository,
                        MvzRepository mvzRepository,
                        ContractRepository contractRepository) {
        this.requestRepository = requestRepository;
        this.requestService = requestService;
        this.budgetItemRepository = budgetItemRepository;
        this.boArticleRepository = boArticleRepository;
        this.cfoRepository = cfoRepository;
        this.mvzRepository = mvzRepository;
        this.contractRepository = contractRepository;

        setSizeFull();
        configureGrid();
        Button add = new Button("Создать", e -> openWizard(new Request()));
        add(add, grid);
        expand(grid);
        updateList();
    }

    private void configureGrid() {
        grid.addColumn(Request::getNumber).setHeader("Номер");
        grid.addColumn(r -> r.getBudgetItem() != null ? r.getBudgetItem().getName() : "").setHeader("БДЗ");
        grid.addColumn(r -> r.getCfo() != null ? r.getCfo().getName() : "").setHeader("ЦФО");
        grid.addColumn(r -> r.getMvz() != null ? r.getMvz().getName() : "").setHeader("МВЗ");
        grid.addColumn(Request::getVgo).setHeader("ВГО");
        grid.addColumn(r -> r.getBoArticle() != null ? r.getBoArticle().getName() : "").setHeader("БО");
        grid.addColumn(r -> r.getContract() != null ? r.getContract().getName() : "").setHeader("Контрагент");
        grid.addColumn(Request::getAmount).setHeader("Сумма");
        grid.addColumn(Request::getAmountWithoutVat).setHeader("Сумма без НДС");
        grid.addColumn(Request::getSubject).setHeader("Предмет");
        grid.addColumn(Request::getPeriod).setHeader("Период");
        grid.addColumn(Request::isIntroductoryObject).setHeader("Вводный объект");
        grid.addColumn(Request::getProcurementMethod).setHeader("Способ закупки");
        grid.setSizeFull();
    }

    private void updateList() {
        grid.setItems(requestRepository.findAll());
    }

    private void openWizard(Request request) {
        new RequestWizard(request).open();
    }

    private class RequestWizard extends Dialog {
        private final Request bean;
        private final Binder<Request> binder = new Binder<>(Request.class);
        private int step = 0;

        // step1
        private final ComboBox<BudgetItem> budgetItem = new ComboBox<>("Статья БДЗ");
        private final ComboBox<BoArticle> boArticle = new ComboBox<>("Статья БО");
        private final TextField zgdField = new TextField("Курирующий ЗГД");
        private final FormLayout step1 = new FormLayout(budgetItem, boArticle, zgdField);

        // step2
        private final ComboBox<Cfo> cfo = new ComboBox<>("ЦФО");
        private final ComboBox<Mvz> mvz = new ComboBox<>("МВЗ");
        private final FormLayout step2 = new FormLayout(cfo, mvz);

        // step3
        private final TextField vgo = new TextField("ВГО");
        private final BigDecimalField amount = new BigDecimalField("Сумма");
        private final BigDecimalField amountWithoutVat = new BigDecimalField("Сумма без НДС");
        private final TextField subject = new TextField("Предмет договора");
        private final TextField period = new TextField("Период");
        private final Checkbox intro = new Checkbox("Вводный объект");
        private final TextField procurement = new TextField("Способ закупки");
        private final TextField contractName = new TextField("Контрагент");
        private final TextField internalNumber = new TextField("Номер внутр.");
        private final TextField externalNumber = new TextField("Номер внешн.");
        private final com.vaadin.flow.component.datepicker.DatePicker contractDate = new com.vaadin.flow.component.datepicker.DatePicker("Дата договора");
        private final TextField responsible = new TextField("Ответственный");
        private final FormLayout step3 = new FormLayout(contractName, internalNumber, externalNumber, contractDate, responsible, vgo,
                amount, amountWithoutVat, subject, period, intro, procurement);

        private final Button back = new Button("Назад", e -> back());
        private final Button next = new Button("Далее", e -> next());
        private final Button save = new Button("Сохранить", e -> save());

        RequestWizard(Request bean) {
            this.bean = bean;
            setWidth("700px");

            budgetItem.setItems(budgetItemRepository.findAll());
            budgetItem.setItemLabelGenerator(BudgetItem::getName);
            budgetItem.addValueChangeListener(e -> {
                var list = boArticleRepository.findAll().stream()
                        .filter(b -> b.getBudgetItem() != null && b.getBudgetItem().equals(e.getValue()))
                        .collect(Collectors.toList());
                boArticle.setItems(list);
                if (e.getValue() != null && e.getValue().getZgd() != null) {
                    var z = e.getValue().getZgd();
                    zgdField.setValue(z.getFio() + " (" + z.getDepartment() + ")");
                } else {
                    zgdField.clear();
                }
            });
            boArticle.setItemLabelGenerator(BoArticle::getName);
            zgdField.setReadOnly(true);

            cfo.setItems(cfoRepository.findAll());
            cfo.setItemLabelGenerator(Cfo::getName);
            cfo.addValueChangeListener(e -> {
                var list = mvzRepository.findAll().stream()
                        .filter(m -> m.getCfo() != null && m.getCfo().equals(e.getValue()))
                        .collect(Collectors.toList());
                mvz.setItems(list);
            });
            mvz.setItemLabelGenerator(Mvz::getName);

            binder.forField(budgetItem).bind(Request::getBudgetItem, Request::setBudgetItem);
            binder.forField(boArticle).bind(Request::getBoArticle, Request::setBoArticle);
            binder.forField(cfo).bind(Request::getCfo, Request::setCfo);
            binder.forField(mvz).bind(Request::getMvz, Request::setMvz);
            binder.forField(vgo).bind(Request::getVgo, Request::setVgo);
            binder.forField(amount).bind(Request::getAmount, Request::setAmount);
            binder.forField(amountWithoutVat).bind(Request::getAmountWithoutVat, Request::setAmountWithoutVat);
            binder.forField(subject).bind(Request::getSubject, Request::setSubject);
            binder.forField(period).bind(Request::getPeriod, Request::setPeriod);
            binder.forField(intro).bind(Request::isIntroductoryObject, Request::setIntroductoryObject);
            binder.forField(procurement).bind(Request::getProcurementMethod, Request::setProcurementMethod);

            add(step1, createButtons());
            updateButtons();
        }

        private HorizontalLayout createButtons() {
            return new HorizontalLayout(back, next, save);
        }

        private void next() {
            if (step == 0) {
                remove(step1);
                addComponentAtIndex(0, step2);
                step++;
            } else if (step == 1) {
                remove(step2);
                addComponentAtIndex(0, step3);
                step++;
            }
            updateButtons();
        }

        private void back() {
            if (step == 1) {
                remove(step2);
                addComponentAtIndex(0, step1);
                step--;
            } else if (step == 2) {
                remove(step3);
                addComponentAtIndex(0, step2);
                step--;
            }
            updateButtons();
        }

        private void updateButtons() {
            back.setEnabled(step > 0);
            next.setVisible(step < 2);
            save.setVisible(step == 2);
        }

        private void save() {
            if (binder.writeBeanIfValid(bean)) {
                Contract contract = bean.getContract();
                if (contract == null) {
                    contract = new Contract();
                }
                contract.setName(contractName.getValue());
                contract.setInternalNumber(internalNumber.getValue());
                contract.setExternalNumber(externalNumber.getValue());
                contract.setDate(contractDate.getValue());
                contract.setResponsible(responsible.getValue());
                bean.setContract(contract);
                requestService.save(bean);
                updateList();
                close();
            }
        }
    }
}
