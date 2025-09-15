package com.example.budget.ui;

import com.example.budget.domain.*;
import com.example.budget.repo.BdzRepository;
import com.example.budget.repo.BoRepository;
import com.example.budget.repo.CfoRepository;
import com.example.budget.repo.MvzRepository;
import com.example.budget.service.RequestService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.spring.annotation.UIScope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Component
@UIScope
public class RequestsView extends VerticalLayout {

    private final RequestService requestService;
    private final BdzRepository bdzRepository;
    private final BoRepository boRepository;
    private final CfoRepository cfoRepository;
    private final MvzRepository mvzRepository;

    private final Grid<Request> grid = new Grid<>(Request.class, false);

    public RequestsView(RequestService requestService, BdzRepository bdzRepository,
                        BoRepository boRepository, CfoRepository cfoRepository, MvzRepository mvzRepository) {
        this.requestService = requestService;
        this.bdzRepository = bdzRepository;
        this.boRepository = boRepository;
        this.cfoRepository = cfoRepository;
        this.mvzRepository = mvzRepository;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        buildGrid();

        Button create = new Button("Создать");
        Button delete = new Button("Удалить выбранные");
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        create.addClickListener(e -> openWizard());
        delete.addClickListener(e -> {
            grid.getSelectedItems().forEach(r -> requestService.deleteById(r.getId()));
            reload();
        });

        Div gridWrapper = new Div(grid);
        gridWrapper.setSizeFull();
        gridWrapper.getStyle().set("overflow", "auto");
        grid.setSizeFull();

        add(new HorizontalLayout(create, delete), gridWrapper);
        setFlexGrow(1, gridWrapper);
        reload();
    }

    private void buildGrid() {
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setWidthFull();
        grid.addColumn(Request::getNumber)
                .setHeader("Номер")
                .setAutoWidth(true)
                .setFlexGrow(0);
        grid.addColumn(r -> r.getBdz() != null ? r.getBdz().getName() : "—")
                .setHeader("БДЗ")
                .setAutoWidth(true)
                .setFlexGrow(0);
        grid.addColumn(r -> r.getCfo() != null ? r.getCfo().getName() : "—")
                .setHeader("ЦФО")
                .setAutoWidth(true)
                .setFlexGrow(0);
        grid.addColumn(r -> r.getMvz() != null ? r.getMvz().getName() : "—")
                .setHeader("МВЗ")
                .setAutoWidth(true)
                .setFlexGrow(0);
        grid.addColumn(Request::getVgo)
                .setHeader("ВГО")
                .setAutoWidth(true)
                .setFlexGrow(0);
        grid.addColumn(r -> r.getBo() != null ? r.getBo().getName() : "—")
                .setHeader("БО")
                .setAutoWidth(true)
                .setFlexGrow(0);
        grid.addColumn(r -> r.getContract() != null ? r.getContract().getName() : "—")
                .setHeader("Контрагент")
                .setAutoWidth(true)
                .setFlexGrow(0);
        grid.addColumn(r -> r.getAmount() != null ? r.getAmount().toPlainString() : "—")
                .setHeader("Сумма")
                .setAutoWidth(true)
                .setFlexGrow(0);
        grid.addColumn(r -> r.getAmountNoVat() != null ? r.getAmountNoVat().toPlainString() : "—")
                .setHeader("Сумма без НДС")
                .setAutoWidth(true)
                .setFlexGrow(0);
        grid.addColumn(Request::getSubject)
                .setHeader("Предмет")
                .setAutoWidth(true)
                .setFlexGrow(0);
        grid.addColumn(Request::getPeriod)
                .setHeader("Период")
                .setAutoWidth(true)
                .setFlexGrow(0);
        grid.addColumn(r -> r.isInputObject() ? "Да" : "Нет")
                .setHeader("Вводный объект")
                .setAutoWidth(true)
                .setFlexGrow(0);
        grid.addColumn(Request::getProcurementMethod)
                .setHeader("Способ закупки")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addItemClickListener(e -> openCard(e.getItem()));
    }

    private void reload() {
        grid.setItems(requestService.findAll());
    }

    private void openCard(Request entity) {
        Dialog d = new Dialog("Заявка № " + entity.getNumber());
        d.setWidth("900px");

        TextField bdzt = new TextField("БДЗ");
        bdzt.setValue(entity.getBdz()!=null ? entity.getBdz().getName() : "");
        bdzt.setReadOnly(true);
        TextField bot = new TextField("БО");
        bot.setValue(entity.getBo()!=null ? entity.getBo().getName() : "");
        bot.setReadOnly(true);
        TextField zgd = new TextField("ЗГД (из БДЗ)");
        zgd.setValue(entity.getZgd()!=null ? entity.getZgd().getFullName() : "");
        zgd.setReadOnly(true);

        TextField cfot = new TextField("ЦФО");
        cfot.setValue(entity.getCfo()!=null ? entity.getCfo().getName() : "");
        cfot.setReadOnly(true);
        TextField mvzt = new TextField("МВЗ");
        mvzt.setValue(entity.getMvz()!=null ? entity.getMvz().getName() : "");
        mvzt.setReadOnly(true);

        TextField vgo = new TextField("ВГО"); vgo.setValue(entity.getVgo()==null? "" : entity.getVgo()); vgo.setReadOnly(true);
        TextField amount = new TextField("Сумма (млн)"); amount.setValue(entity.getAmount()!=null? entity.getAmount().toPlainString():""); amount.setReadOnly(true);
        TextField amountNoVat = new TextField("Без НДС (млн)"); amountNoVat.setValue(entity.getAmountNoVat()!=null? entity.getAmountNoVat().toPlainString():""); amountNoVat.setReadOnly(true);
        TextField subject = new TextField("Предмет договора"); subject.setValue(entity.getSubject()==null? "" : entity.getSubject()); subject.setReadOnly(true);
        TextField period = new TextField("Период (месяц)"); period.setValue(entity.getPeriod()==null? "" : entity.getPeriod()); period.setReadOnly(true);
        TextField input = new TextField("Вводный объект"); input.setValue(entity.isInputObject()? "Да":"Нет"); input.setReadOnly(true);
        TextField pm = new TextField("Способ закупки"); pm.setValue(entity.getProcurementMethod()==null? "" : entity.getProcurementMethod()); pm.setReadOnly(true);

        Button edit = new Button("Редактировать", e -> { d.close(); openEdit(entity);});
        Button del = new Button("Удалить", e -> { requestService.deleteById(entity.getId()); d.close(); reload(); });
        del.addThemeVariants(ButtonVariant.LUMO_ERROR);
        Button close = new Button("Закрыть", e -> d.close());

        d.add(new FormLayout(bdzt, bot, zgd, cfot, mvzt, vgo, amount, amountNoVat, subject, period, input, pm),
                new HorizontalLayout(edit, del, close));
        d.open();
    }

    private void openEdit(Request entity) {
        Dialog d = new Dialog("Редактирование заявки № " + entity.getNumber());
        d.setWidth("900px");
        Binder<Request> binder = new Binder<>(Request.class);
        binder.setBean(entity);

        TextField vgo = new TextField("ВГО");
        NumberField amount = new NumberField("Сумма (млн)");
        NumberField amountNoVat = new NumberField("Без НДС (млн)");
        TextField subject = new TextField("Предмет договора");
        TextField period = new TextField("Период (месяц)");
        TextField pm = new TextField("Способ закупки");
        com.vaadin.flow.component.checkbox.Checkbox input = new com.vaadin.flow.component.checkbox.Checkbox("Вводный объект");

        binder.bind(vgo, Request::getVgo, Request::setVgo);
        binder.forField(amount).bind(
                r -> r.getAmount() != null ? r.getAmount().doubleValue() : null,
                (r, v) -> r.setAmount(v != null ? new BigDecimal(v) : null));
        binder.forField(amountNoVat).bind(
                r -> r.getAmountNoVat() != null ? r.getAmountNoVat().doubleValue() : null,
                (r, v) -> r.setAmountNoVat(v != null ? new BigDecimal(v) : null));
        binder.bind(subject, Request::getSubject, Request::setSubject);
        binder.bind(period, Request::getPeriod, Request::setPeriod);
        binder.bind(pm, Request::getProcurementMethod, Request::setProcurementMethod);
        binder.bind(input, Request::isInputObject, Request::setInputObject);

        Button save = new Button("Сохранить", e -> { requestService.save(binder.getBean()); d.close(); reload(); });
        Button close = new Button("Закрыть", e -> d.close());
        d.add(new FormLayout(vgo, amount, amountNoVat, subject, period, pm, input), new HorizontalLayout(save, close));
        d.open();
    }

    // === Wizard ===
    private void openWizard() {
        Dialog d = new Dialog("Создание заявки");
        d.setWidth("900px");

        // step 1
        ComboBox<Bdz> bdz = new ComboBox<>("БДЗ");
        bdz.setItems(bdzRepository.findAll());
        bdz.setItemLabelGenerator(Bdz::getName);
        ComboBox<Bo> bo = new ComboBox<>("БО");
        bo.setItemLabelGenerator(Bo::getName);
        TextField zgd = new TextField("ЗГД (автоподстановка)");
        zgd.setReadOnly(true);
        bdz.addValueChangeListener(e -> {
            Bo selected = null;
            if (e.getValue() != null) {
                bo.setItems(boRepository.findByBdzId(e.getValue().getId()));
                if (e.getValue().getZgd() != null) {
                    zgd.setValue(e.getValue().getZgd().getFullName());
                } else {
                    zgd.setValue("");
                }
            } else {
                bo.clear();
                bo.setItems(List.of());
                zgd.clear();
            }
        });

        // step 2
        ComboBox<Cfo> cfo = new ComboBox<>("ЦФО");
        cfo.setItems(cfoRepository.findAll());
        cfo.setItemLabelGenerator(Cfo::getName);
        ComboBox<Mvz> mvz = new ComboBox<>("МВЗ");
        mvz.setItemLabelGenerator(Mvz::getName);
        cfo.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                mvz.setItems(mvzRepository.findByCfoId(e.getValue().getId()));
            } else {
                mvz.clear();
                mvz.setItems(List.of());
            }
        });

        // step 3
        TextField vgo = new TextField("ВГО");
        NumberField amount = new NumberField("Сумма (млн)");
        NumberField amountNoVat = new NumberField("Без НДС (млн)");
        TextField subject = new TextField("Предмет договора");
        TextField period = new TextField("Период (месяц)");
        TextField pm = new TextField("Способ закупки");
        com.vaadin.flow.component.checkbox.Checkbox input = new com.vaadin.flow.component.checkbox.Checkbox("Вводный объект");

        // simple step control
        Span step = new Span("Шаг 1 из 3");
        VerticalLayout step1 = new VerticalLayout(bdz, bo, zgd);
        VerticalLayout step2 = new VerticalLayout(cfo, mvz);
        VerticalLayout step3 = new VerticalLayout(vgo, amount, amountNoVat, subject, period, pm, input);
        step2.setVisible(false);
        step3.setVisible(false);

        Button back = new Button("Назад", e -> {
            if (step3.isVisible()) { step3.setVisible(false); step2.setVisible(true); step.setText("Шаг 2 из 3"); }
            else if (step2.isVisible()) { step2.setVisible(false); step1.setVisible(true); step.setText("Шаг 1 из 3"); }
        });
        Button next = new Button("Далее", e -> {
            if (step1.isVisible()) { step1.setVisible(false); step2.setVisible(true); step.setText("Шаг 2 из 3"); }
            else if (step2.isVisible()) { step2.setVisible(false); step3.setVisible(true); step.setText("Шаг 3 из 3"); }
        });
        Button save = new Button("Сохранить", e -> {
            Request r = new Request();
            r.setBdz(bdz.getValue());
            r.setBo(bo.getValue());
            if (bdz.getValue()!=null) r.setZgd(bdz.getValue().getZgd());
            r.setCfo(cfo.getValue());
            r.setMvz(mvz.getValue());
            r.setVgo(vgo.getValue());
            r.setAmount(amount.getValue() != null ? java.math.BigDecimal.valueOf(amount.getValue()) : null);
            r.setAmountNoVat(amountNoVat.getValue() != null ? java.math.BigDecimal.valueOf(amountNoVat.getValue()) : null);
            r.setSubject(subject.getValue());
            r.setPeriod(period.getValue());
            r.setInputObject(input.getValue());
            r.setProcurementMethod(pm.getValue());
            requestService.save(r);
            d.close();
            reload();
        });
        Button close = new Button("Закрыть", e -> d.close());

        d.add(step, step1, step2, step3, new HorizontalLayout(back, next, save, close));
        d.open();
    }
}
