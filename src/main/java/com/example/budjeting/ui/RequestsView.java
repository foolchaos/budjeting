package com.example.budjeting.ui;

import com.example.budjeting.model.*;
import com.example.budjeting.repository.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * View for managing budget requests.
 */
@Route(value = "requests", layout = MainLayout.class)
public class RequestsView extends VerticalLayout {
    private final AppRequestRepository repo;
    private final ListDataProvider<AppRequest> dataProvider;
    private final Grid<AppRequest> grid = new Grid<>(AppRequest.class, false);

    // filter fields
    private final TextField numberFilter = new TextField();
    private final ComboBox<BudgetArticle> budgetFilter = new ComboBox<>();
    private final ComboBox<CFO> cfoFilter = new ComboBox<>();
    private final ComboBox<MVZ> mvzFilter = new ComboBox<>();
    private final TextField vgoFilter = new TextField();
    private final ComboBox<BOArticle> boFilter = new ComboBox<>();
    private final TextField contractFilter = new TextField();
    private final TextField amountFilter = new TextField();
    private final TextField amountWithoutVatFilter = new TextField();
    private final TextField subjectFilter = new TextField();
    private final TextField periodFilter = new TextField();
    private final Checkbox inputObjectFilter = new Checkbox();
    private final TextField procurementFilter = new TextField();

    public RequestsView(AppRequestRepository repo,
                        BudgetArticleRepository budgetRepo,
                        BOArticleRepository boRepo,
                        SupervisorRepository supervisorRepo,
                        CFORepository cfoRepo,
                        MVZRepository mvzRepo,
                        ContractRepository contractRepo) {
        this.repo = repo;
        setSizeFull();
        dataProvider = new ListDataProvider<>(repo.findAll());
        grid.setDataProvider(dataProvider);
        grid.setSelectionMode(Grid.SelectionMode.MULTI);

        grid.addColumn(AppRequest::getNumber).setHeader("Номер").setKey("number");
        grid.addColumn(r -> r.getBudgetArticle() != null ? r.getBudgetArticle().getName() : "").setHeader("БДЗ").setKey("budget");
        grid.addColumn(r -> r.getCfo() != null ? r.getCfo().getName() : "").setHeader("ЦФО").setKey("cfo");
        grid.addColumn(r -> r.getMvz() != null ? r.getMvz().getName() : "").setHeader("МВЗ").setKey("mvz");
        grid.addColumn(AppRequest::getVgo).setHeader("ВГО").setKey("vgo");
        grid.addColumn(r -> r.getBoArticle() != null ? r.getBoArticle().getName() : "").setHeader("БО").setKey("bo");
        grid.addColumn(r -> r.getContract() != null ? r.getContract().getName() : "").setHeader("Контрагент").setKey("contract");
        grid.addColumn(AppRequest::getAmount).setHeader("Сумма").setKey("amount");
        grid.addColumn(AppRequest::getAmountWithoutVat).setHeader("Сумма без НДС").setKey("amountNoVat");
        grid.addColumn(AppRequest::getSubject).setHeader("Предмет").setKey("subject");
        grid.addColumn(AppRequest::getPeriod).setHeader("Период").setKey("period");
        grid.addColumn(r -> r.isInputObject()).setHeader("Вводный объект").setKey("input");
        grid.addColumn(AppRequest::getProcurementMethod).setHeader("Способ закупки").setKey("procurement");

        // filters
        HeaderRow filterRow = grid.appendHeaderRow();
        configureFilter(numberFilter, filterRow, "number");
        configureFilterCombo(budgetFilter, budgetRepo.findAll(), BudgetArticle::getName, filterRow, "budget");
        configureFilterCombo(cfoFilter, cfoRepo.findAll(), CFO::getName, filterRow, "cfo");
        configureFilterCombo(mvzFilter, mvzRepo.findAll(), MVZ::getName, filterRow, "mvz");
        configureFilter(vgoFilter, filterRow, "vgo");
        configureFilterCombo(boFilter, boRepo.findAll(), BOArticle::getName, filterRow, "bo");
        configureFilter(contractFilter, filterRow, "contract");
        configureFilter(amountFilter, filterRow, "amount");
        configureFilter(amountWithoutVatFilter, filterRow, "amountNoVat");
        configureFilter(subjectFilter, filterRow, "subject");
        configureFilter(periodFilter, filterRow, "period");
        filterRow.getCell(grid.getColumnByKey("input")).setComponent(inputObjectFilter);
        inputObjectFilter.addValueChangeListener(e -> applyFilters());
        configureFilter(procurementFilter, filterRow, "procurement");

        Button create = new Button("Создать", e -> openWizard(budgetRepo, boRepo, supervisorRepo, cfoRepo, mvzRepo, contractRepo));
        Button delete = new Button("Удалить выбранные", e -> {
            repo.deleteAll(grid.getSelectedItems());
            refresh();
        });
        HorizontalLayout actions = new HorizontalLayout(create, delete);
        add(actions, grid);
        setFlexGrow(1, grid);
    }

    private <T> void configureFilterCombo(ComboBox<T> combo, List<T> items, java.util.function.Function<T, String> label,
                                          HeaderRow row, String key) {
        combo.setItems(items);
        combo.setItemLabelGenerator(label::apply);
        combo.addValueChangeListener(e -> applyFilters());
        row.getCell(grid.getColumnByKey(key)).setComponent(combo);
    }

    private void configureFilter(TextField field, HeaderRow row, String key) {
        field.setValueChangeMode(ValueChangeMode.EAGER);
        field.addValueChangeListener(e -> applyFilters());
        row.getCell(grid.getColumnByKey(key)).setComponent(field);
    }

    private void applyFilters() {
        dataProvider.setFilter(item -> {
            if (!numberFilter.isEmpty() && (item.getNumber() == null || !item.getNumber().contains(numberFilter.getValue()))) {
                return false;
            }
            if (budgetFilter.getValue() != null && !Objects.equals(item.getBudgetArticle(), budgetFilter.getValue())) {
                return false;
            }
            if (cfoFilter.getValue() != null && !Objects.equals(item.getCfo(), cfoFilter.getValue())) {
                return false;
            }
            if (mvzFilter.getValue() != null && !Objects.equals(item.getMvz(), mvzFilter.getValue())) {
                return false;
            }
            if (!vgoFilter.isEmpty() && (item.getVgo() == null || !item.getVgo().contains(vgoFilter.getValue()))) {
                return false;
            }
            if (boFilter.getValue() != null && !Objects.equals(item.getBoArticle(), boFilter.getValue())) {
                return false;
            }
            if (!contractFilter.isEmpty() && (item.getContract() == null || !item.getContract().getName().contains(contractFilter.getValue()))) {
                return false;
            }
            if (!amountFilter.isEmpty() && (item.getAmount() == null || !item.getAmount().toString().contains(amountFilter.getValue()))) {
                return false;
            }
            if (!amountWithoutVatFilter.isEmpty() && (item.getAmountWithoutVat() == null || !item.getAmountWithoutVat().toString().contains(amountWithoutVatFilter.getValue()))) {
                return false;
            }
            if (!subjectFilter.isEmpty() && (item.getSubject() == null || !item.getSubject().contains(subjectFilter.getValue()))) {
                return false;
            }
            if (!periodFilter.isEmpty() && (item.getPeriod() == null || !item.getPeriod().contains(periodFilter.getValue()))) {
                return false;
            }
            if (inputObjectFilter.getValue() != null && inputObjectFilter.getValue() && !item.isInputObject()) {
                return false;
            }
            if (!procurementFilter.isEmpty() && (item.getProcurementMethod() == null || !item.getProcurementMethod().contains(procurementFilter.getValue()))) {
                return false;
            }
            return true;
        });
    }

    private void refresh() {
        dataProvider.getItems().clear();
        dataProvider.getItems().addAll(repo.findAll());
        dataProvider.refreshAll();
        applyFilters();
    }

    private void openWizard(BudgetArticleRepository budgetRepo,
                            BOArticleRepository boRepo,
                            SupervisorRepository supervisorRepo,
                            CFORepository cfoRepo,
                            MVZRepository mvzRepo,
                            ContractRepository contractRepo) {
        Dialog dialog = new Dialog();
        VerticalLayout step1 = new VerticalLayout();
        ComboBox<BudgetArticle> budget = new ComboBox<>("Статья БДЗ");
        budget.setItems(budgetRepo.findAll());
        budget.setItemLabelGenerator(BudgetArticle::getName);
        ComboBox<BOArticle> bo = new ComboBox<>("Статья БО");
        bo.setItemLabelGenerator(BOArticle::getName);
        ComboBox<Supervisor> supervisor = new ComboBox<>("Курирующий ЗГД");
        supervisor.setItemLabelGenerator(Supervisor::getFullName);
        supervisor.setReadOnly(true);
        budget.addValueChangeListener(e -> {
            BudgetArticle ba = e.getValue();
            if (ba != null) {
                bo.setItems(ba.getBoArticles());
                Supervisor sup = ba.getSupervisor();
                supervisor.setItems(sup != null ? List.of(sup) : List.of());
                supervisor.setValue(sup);
            } else {
                bo.clear();
                bo.setItems(List.of());
                supervisor.clear();
                supervisor.setItems(List.of());
            }
        });
        step1.add(budget, bo, supervisor);

        VerticalLayout step2 = new VerticalLayout();
        ComboBox<CFO> cfo = new ComboBox<>("ЦФО");
        cfo.setItems(cfoRepo.findAll());
        cfo.setItemLabelGenerator(CFO::getName);
        ComboBox<MVZ> mvz = new ComboBox<>("МВЗ");
        mvz.setItemLabelGenerator(MVZ::getName);
        cfo.addValueChangeListener(e -> {
            CFO val = e.getValue();
            if (val != null) {
                mvz.setItems(mvzRepo.findAll().stream().filter(m -> Objects.equals(m.getCfo(), val)).toList());
            } else {
                mvz.setItems(List.of());
            }
        });
        step2.add(cfo, mvz);

        VerticalLayout step3 = new VerticalLayout();
        TextField vgo = new TextField("ВГО");
        TextField amount = new TextField("Сумма");
        TextField amountNoVat = new TextField("Сумма без НДС");
        TextField subject = new TextField("Предмет договора");
        TextField period = new TextField("Период");
        Checkbox inputObject = new Checkbox("Вводный объект");
        TextField procurement = new TextField("Способ закупки");
        ComboBox<Contract> contract = new ComboBox<>("Договор");
        contract.setItems(contractRepo.findAll());
        contract.setItemLabelGenerator(Contract::getName);
        step3.add(vgo, amount, amountNoVat, subject, period, inputObject, procurement, contract);

        step2.setVisible(false);
        step3.setVisible(false);
        AtomicInteger step = new AtomicInteger(1);
        Button next = new Button("Далее");
        Button back = new Button("Назад");
        back.addClickListener(e -> {
            if (step.get() == 2) {
                step2.setVisible(false);
                step1.setVisible(true);
                back.setEnabled(false);
            } else if (step.get() == 3) {
                step3.setVisible(false);
                step2.setVisible(true);
                next.setEnabled(true);
                step.set(2);
                return;
            }
            step.set(1);
        });
        back.setEnabled(false);
        next.addClickListener(e -> {
            if (step.get() == 1) {
                step1.setVisible(false);
                step2.setVisible(true);
                back.setEnabled(true);
                step.set(2);
            } else if (step.get() == 2) {
                step2.setVisible(false);
                step3.setVisible(true);
                next.setEnabled(false);
                step.set(3);
            }
        });
        Button save = new Button("Сохранить", e -> {
            AppRequest req = new AppRequest();
            req.setBudgetArticle(budget.getValue());
            req.setBoArticle(bo.getValue());
            req.setSupervisor(supervisor.getValue());
            req.setCfo(cfo.getValue());
            req.setMvz(mvz.getValue());
            req.setContract(contract.getValue());
            req.setVgo(vgo.getValue());
            if (!amount.isEmpty()) {
                req.setAmount(new BigDecimal(amount.getValue()));
            }
            if (!amountNoVat.isEmpty()) {
                req.setAmountWithoutVat(new BigDecimal(amountNoVat.getValue()));
            }
            req.setSubject(subject.getValue());
            req.setPeriod(period.getValue());
            req.setInputObject(inputObject.getValue());
            req.setProcurementMethod(procurement.getValue());
            repo.save(req);
            refresh();
            dialog.close();
        });
        HorizontalLayout buttons = new HorizontalLayout(back, next, save);
        dialog.add(step1, step2, step3, buttons);
        dialog.open();
    }
}
