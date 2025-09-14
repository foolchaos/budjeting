package com.example.budjeting.ui;

import com.example.budjeting.entity.*;
import com.example.budjeting.repository.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.Route;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Route(value = "requests", layout = MainLayout.class)
public class RequestsView extends VerticalLayout {

    private final RequestRepository requestRepository;
    private final BudgetItemRepository budgetItemRepository;
    private final BoArticleRepository boArticleRepository;
    private final CfoRepository cfoRepository;
    private final MvzRepository mvzRepository;
    private final ContractRepository contractRepository;

    public RequestsView(RequestRepository requestRepository,
                        BudgetItemRepository budgetItemRepository,
                        BoArticleRepository boArticleRepository,
                        CfoRepository cfoRepository,
                        MvzRepository mvzRepository,
                        ContractRepository contractRepository) {
        this.requestRepository = requestRepository;
        this.budgetItemRepository = budgetItemRepository;
        this.boArticleRepository = boArticleRepository;
        this.cfoRepository = cfoRepository;
        this.mvzRepository = mvzRepository;
        this.contractRepository = contractRepository;

        setSizeFull();
        Grid<Request> grid = new Grid<>(Request.class, false);
        grid.addColumn(Request::getNumber).setHeader("Номер").setKey("number");
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

        ListDataProvider<Request> provider = new ListDataProvider<>(requestRepository.findAll());
        grid.setDataProvider(provider);
        HeaderRow filterRow = grid.appendHeaderRow();
        TextField numberFilter = new TextField();
        numberFilter.addValueChangeListener(e -> provider.addFilter(req -> req.getNumber() != null && req.getNumber().contains(e.getValue())));
        filterRow.getCell(grid.getColumnByKey("number")).setComponent(numberFilter);
        grid.setSelectionMode(Grid.SelectionMode.MULTI);

        Button add = new Button("Создать", e -> openWizard(new Request(), provider));
        Button delete = new Button("Удалить", e -> {
            grid.getSelectedItems().forEach(requestRepository::delete);
            provider.getItems().clear();
            provider.getItems().addAll(requestRepository.findAll());
            provider.refreshAll();
        });
        add(new HorizontalLayout(add, delete), grid);
    }

    private void openWizard(Request request, ListDataProvider<Request> provider) {
        Dialog dialog = new Dialog();
        Tab step1 = new Tab("1. БДЗ/БО");
        Tab step2 = new Tab("2. ЦФО/МВЗ");
        Tab step3 = new Tab("3. Прочее");
        Tabs steps = new Tabs(step1, step2, step3);
        VerticalLayout pages = new VerticalLayout();
        pages.setSizeFull();

        // Step 1
        FormLayout step1Form = new FormLayout();
        ComboBox<BudgetItem> budgetItem = new ComboBox<>("БДЗ");
        budgetItem.setItems(budgetItemRepository.findAll());
        budgetItem.setItemLabelGenerator(BudgetItem::getName);
        ComboBox<BoArticle> bo = new ComboBox<>("БО");
        bo.setItemLabelGenerator(BoArticle::getName);
        TextField zgdField = new TextField("ЗГД");
        zgdField.setReadOnly(true);
        budgetItem.addValueChangeListener(ev -> {
            if (ev.getValue() != null) {
                List<BoArticle> list = boArticleRepository.findAll().stream()
                        .filter(b -> b.getBudgetItem() != null && b.getBudgetItem().equals(ev.getValue()))
                        .toList();
                bo.setItems(list);
                if (ev.getValue().getZgd() != null) {
                    Zgd z = ev.getValue().getZgd();
                    zgdField.setValue(z.getFullName() + ", " + z.getDepartment());
                } else {
                    zgdField.clear();
                }
            }
        });
        step1Form.add(budgetItem, bo, zgdField);

        // Step 2
        FormLayout step2Form = new FormLayout();
        ComboBox<Cfo> cfo = new ComboBox<>("ЦФО");
        cfo.setItems(cfoRepository.findAll());
        cfo.setItemLabelGenerator(Cfo::getName);
        ComboBox<Mvz> mvz = new ComboBox<>("МВЗ");
        mvz.setItemLabelGenerator(Mvz::getName);
        cfo.addValueChangeListener(ev -> {
            if (ev.getValue() != null) {
                List<Mvz> list = mvzRepository.findAll().stream()
                        .filter(m -> m.getCfo() != null && m.getCfo().equals(ev.getValue()))
                        .toList();
                mvz.setItems(list);
            }
        });
        step2Form.add(cfo, mvz);

        // Step 3
        FormLayout step3Form = new FormLayout();
        ComboBox<Contract> contract = new ComboBox<>("Договор");
        contract.setItems(contractRepository.findAll());
        contract.setItemLabelGenerator(Contract::getName);
        TextField vgo = new TextField("ВГО");
        TextField amount = new TextField("Сумма");
        TextField amountNoVat = new TextField("Сумма без НДС");
        TextField subject = new TextField("Предмет");
        TextField period = new TextField("Период");
        Checkbox intro = new Checkbox("Вводный объект");
        TextField method = new TextField("Способ закупки");
        step3Form.add(contract, vgo, amount, amountNoVat, subject, period, intro, method);

        Map<Tab, FormLayout> map = Map.of(step1, step1Form, step2, step2Form, step3, step3Form);
        steps.addSelectedChangeListener(e -> {
            pages.removeAll();
            pages.add(map.get(e.getSelectedTab()));
        });
        pages.add(step1Form);

        Binder<Request> binder = new Binder<>(Request.class);
        binder.bind(budgetItem, Request::getBudgetItem, Request::setBudgetItem);
        binder.bind(bo, Request::getBoArticle, Request::setBoArticle);
        binder.bind(cfo, Request::getCfo, Request::setCfo);
        binder.bind(mvz, Request::getMvz, Request::setMvz);
        binder.bind(contract, Request::getContract, Request::setContract);
        binder.bind(vgo, Request::getVgo, Request::setVgo);
        binder.bind(subject, Request::getSubject, Request::setSubject);
        binder.bind(period, Request::getPeriod, Request::setPeriod);
        binder.bind(method, Request::getProcurementMethod, Request::setProcurementMethod);
        binder.bind(intro, Request::isIntroductoryObject, Request::setIntroductoryObject);
        binder.forField(amount).bind(r -> r.getAmount() != null ? r.getAmount().toString() : "", (r,v) -> r.setAmount(v != null && !v.isEmpty() ? new BigDecimal(v) : null));
        binder.forField(amountNoVat).bind(r -> r.getAmountWithoutVat() != null ? r.getAmountWithoutVat().toString() : "", (r,v) -> r.setAmountWithoutVat(v != null && !v.isEmpty() ? new BigDecimal(v) : null));
        binder.readBean(request);

        Button back = new Button("Назад");
        Button next = new Button("Далее");
        Button save = new Button("Сохранить");
        back.addClickListener(e -> steps.setSelectedIndex(Math.max(0, steps.getSelectedIndex()-1)));
        next.addClickListener(e -> steps.setSelectedIndex(Math.min(2, steps.getSelectedIndex()+1)));
        save.addClickListener(e -> {
            try {
                binder.writeBean(request);
                requestRepository.save(request);
                provider.getItems().clear();
                provider.getItems().addAll(requestRepository.findAll());
                provider.refreshAll();
                dialog.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        steps.addSelectedChangeListener(e -> {
            back.setEnabled(steps.getSelectedIndex() > 0);
            next.setEnabled(steps.getSelectedIndex() < 2);
            save.setVisible(steps.getSelectedIndex() == 2);
        });
        back.setEnabled(false);
        save.setVisible(false);

        dialog.add(steps, pages, new HorizontalLayout(back, next, save));
        dialog.open();
    }
}
