package com.example.budget.ui;

import com.example.budget.domain.*;
import com.example.budget.service.BdzService;
import com.example.budget.repo.BoRepository;
import com.example.budget.repo.CfoTwoRepository;
import com.example.budget.repo.MvzRepository;
import com.example.budget.repo.ContractRepository;
import com.example.budget.service.RequestService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.spring.annotation.UIScope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@UIScope
public class RequestsView extends VerticalLayout {

    private final RequestService requestService;
    private final BdzService bdzService;
    private final BoRepository boRepository;
    private final CfoTwoRepository cfoTwoRepository;
    private final MvzRepository mvzRepository;
    private final ContractRepository contractRepository;

    private final Grid<Request> grid = new Grid<>(Request.class, false);
    private final List<Request> allRequests = new ArrayList<>();
    private final Select<Integer> pageSizeSelect = new Select<>();
    private final Button prevPage = new Button("Назад");
    private final Button nextPage = new Button("Вперёд");
    private final Span pageInfo = new Span();
    private int currentPage = 0;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public RequestsView(RequestService requestService, BdzService bdzService,
                        BoRepository boRepository, CfoTwoRepository cfoTwoRepository,
                        MvzRepository mvzRepository, ContractRepository contractRepository) {
        this.requestService = requestService;
        this.bdzService = bdzService;
        this.boRepository = boRepository;
        this.cfoTwoRepository = cfoTwoRepository;
        this.mvzRepository = mvzRepository;
        this.contractRepository = contractRepository;

        setSizeFull();
        buildGrid();

        Button create = new Button("Создать");
        Button delete = new Button("Удалить выбранные");
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        create.addClickListener(e -> openWizard());
        delete.addClickListener(e -> {
            grid.getSelectedItems().forEach(r -> requestService.deleteById(r.getId()));
            reload();
        });

        HorizontalLayout actions = new HorizontalLayout(create, delete);
        grid.setSizeFull();

        HorizontalLayout pagination = buildPaginationBar();

        add(actions, grid, pagination);
        setFlexGrow(1, grid);
        setFlexGrow(0, pagination);
        reload();
    }

    private void buildGrid() {
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setWidthFull();
        grid.addColumn(Request::getNumber)
                .setHeader("Номер")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(r -> r.getBdz() != null ? r.getBdz().getName() : "—")
                .setHeader("БДЗ")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(r -> r.getCfo2() != null ? r.getCfo2().getName() : "—")
                .setHeader("ЦФО II")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(r -> r.getMvz() != null ? r.getMvz().getName() : "—")
                .setHeader("МВЗ")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(Request::getVgo)
                .setHeader("ВГО")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(r -> r.getBo() != null ? r.getBo().getName() : "—")
                .setHeader("БО")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(r -> r.getContract() != null ? r.getContract().getName() : "—")
                .setHeader("Контрагент")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(r -> r.getAmount() != null ? r.getAmount().toPlainString() : "—")
                .setHeader("Сумма")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(r -> r.getAmountNoVat() != null ? r.getAmountNoVat().toPlainString() : "—")
                .setHeader("Сумма без НДС")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(Request::getSubject)
                .setHeader("Предмет")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(Request::getPeriod)
                .setHeader("Период")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(r -> r.isInputObject() ? "Да" : "Нет")
                .setHeader("Вводный объект")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(Request::getProcurementMethod)
                .setHeader("Способ закупки")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addItemClickListener(e -> openCard(e.getItem()));
    }

    private HorizontalLayout buildPaginationBar() {
        pageSizeSelect.setLabel("Строк на странице");
        pageSizeSelect.setItems(5, 10, 25, 50);
        pageSizeSelect.setValue(10);
        pageSizeSelect.addValueChangeListener(e -> {
            currentPage = 0;
            updateGridPage();
        });

        prevPage.addClickListener(e -> {
            if (currentPage > 0) {
                currentPage--;
                updateGridPage();
            }
        });

        nextPage.addClickListener(e -> {
            if ((currentPage + 1) * pageSizeSelect.getValue() < allRequests.size()) {
                currentPage++;
                updateGridPage();
            }
        });

        HorizontalLayout pagination = new HorizontalLayout(prevPage, nextPage, pageInfo, pageSizeSelect);
        pagination.setAlignItems(Alignment.CENTER);
        pagination.setWidthFull();
        pageInfo.getStyle().set("margin-left", "auto");
        pageInfo.getStyle().set("margin-right", "var(--lumo-space-m)");

        updateGridPage();
        return pagination;
    }

    private void reload() {
        allRequests.clear();
        allRequests.addAll(requestService.findAll());
        currentPage = 0;
        updateGridPage();
    }

    private void updateGridPage() {
        Integer selectedSize = pageSizeSelect.getValue();
        int pageSize = selectedSize != null ? selectedSize : 10;
        int total = allRequests.size();

        if (total == 0) {
            grid.setItems(List.of());
            pageInfo.setText("0 из 0");
            prevPage.setEnabled(false);
            nextPage.setEnabled(false);
            return;
        }

        int pageCount = (int) Math.ceil((double) total / pageSize);
        if (currentPage >= pageCount) {
            currentPage = Math.max(pageCount - 1, 0);
        }

        int fromIndex = currentPage * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);

        if (fromIndex >= total) {
            currentPage = 0;
            fromIndex = 0;
            toIndex = Math.min(pageSize, total);
        }

        grid.setItems(allRequests.subList(fromIndex, toIndex));
        pageInfo.setText(String.format("%d–%d из %d", fromIndex + 1, toIndex, total));

        prevPage.setEnabled(currentPage > 0);
        nextPage.setEnabled(currentPage < pageCount - 1);
    }

    private void openCard(Request entity) {
        Request detailed = requestService.findDetailedById(entity.getId());
        Dialog d = new Dialog("Заявка № " + detailed.getNumber());
        d.setWidth("900px");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);
        content.setWidthFull();
        content.getStyle().set("max-height", "70vh");
        content.getStyle().set("overflow", "auto");

        content.add(
                infoSection("Основная информация",
                        entry("Номер заявки", detailed.getNumber()),
                        entry("Период (месяц)", detailed.getPeriod()),
                        entry("ВГО", detailed.getVgo()),
                        entry("Предмет договора", detailed.getSubject()),
                        entry("Способ закупки", detailed.getProcurementMethod()),
                        entry("Сумма (млн)", detailed.getAmount()),
                        entry("Сумма без НДС (млн)", detailed.getAmountNoVat()),
                        entry("Вводный объект", detailed.isInputObject())
                ),
                infoSection("ЦФО II и МВЗ",
                        entry("Код ЦФО II", detailed.getCfo2() != null ? detailed.getCfo2().getCode() : null),
                        entry("Наименование ЦФО II", detailed.getCfo2() != null ? detailed.getCfo2().getName() : null),
                        entry("Код МВЗ", detailed.getMvz() != null ? detailed.getMvz().getCode() : null),
                        entry("Наименование МВЗ", detailed.getMvz() != null ? detailed.getMvz().getName() : null)
                ),
                infoSection("БДЗ и БО",
                        entry("Код БДЗ", detailed.getBdz() != null ? detailed.getBdz().getCode() : null),
                        entry("Наименование БДЗ", detailed.getBdz() != null ? detailed.getBdz().getName() : null),
                        entry("Код родительского БДЗ", detailed.getBdz() != null && detailed.getBdz().getParent() != null ? detailed.getBdz().getParent().getCode() : null),
                        entry("Наименование родительского БДЗ", detailed.getBdz() != null && detailed.getBdz().getParent() != null ? detailed.getBdz().getParent().getName() : null),
                        entry("Код БО", detailed.getBo() != null ? detailed.getBo().getCode() : null),
                        entry("Наименование БО", detailed.getBo() != null ? detailed.getBo().getName() : null)
                ),
                infoSection("ЗГД",
                        entry("ФИО", detailed.getZgd() != null ? detailed.getZgd().getFullName() : null),
                        entry("Подразделение", detailed.getZgd() != null ? detailed.getZgd().getDepartment() : null)
                ),
                infoSection("Договор",
                        entry("Наименование", detailed.getContract() != null ? detailed.getContract().getName() : null),
                        entry("Внутренний номер", detailed.getContract() != null ? detailed.getContract().getInternalNumber() : null),
                        entry("Внешний номер", detailed.getContract() != null ? detailed.getContract().getExternalNumber() : null),
                        entry("Дата договора", detailed.getContract() != null ? detailed.getContract().getContractDate() : null),
                        entry("Ответственный", detailed.getContract() != null ? detailed.getContract().getResponsible() : null)
                )
        );

        Button edit = new Button("Редактировать", e -> {
            d.close();
            openEdit(entity);
        });
        Button del = new Button("Удалить", e -> {
            requestService.deleteById(detailed.getId());
            d.close();
            reload();
        });
        del.addThemeVariants(ButtonVariant.LUMO_ERROR);
        Button close = new Button("Закрыть", e -> d.close());

        HorizontalLayout actions = new HorizontalLayout(edit, del, close);
        actions.setSpacing(true);
        actions.setPadding(false);
        actions.setWidthFull();
        actions.setJustifyContentMode(JustifyContentMode.END);

        d.add(content, actions);
        d.open();
    }

    private void openEdit(Request entity) {
        openWizard(requestService.findDetailedById(entity.getId()), true);
    }

    private void openWizard() {
        openWizard(new Request(), false);
    }

    private void openWizard(Request bean, boolean editing) {
        Dialog d = new Dialog(editing ? "Редактирование заявки № " + bean.getNumber() : "Создание заявки");
        d.setWidth("600px");

        Binder<Request> binder = new Binder<>(Request.class);

        ComboBox<CfoTwo> cfo = new ComboBox<>("ЦФО II");
        List<CfoTwo> cfoItems = new ArrayList<>(cfoTwoRepository.findAll());
        if (bean.getCfo2() != null) {
            CfoTwo selectedCfo = findById(cfoItems, CfoTwo::getId, bean.getCfo2().getId());
            if (selectedCfo != null) {
                bean.setCfo2(selectedCfo);
            } else {
                cfoItems.add(bean.getCfo2());
            }
        }
        cfo.setItems(cfoItems);
        cfo.setItemLabelGenerator(c -> formatCodeName(c.getCode(), c.getName()));
        cfo.setWidthFull();
        cfo.setClearButtonVisible(true);

        ComboBox<Mvz> mvz = new ComboBox<>("МВЗ");
        List<Mvz> mvzItems = new ArrayList<>(mvzRepository.findAll());
        if (bean.getMvz() != null) {
            Mvz selectedMvz = findById(mvzItems, Mvz::getId, bean.getMvz().getId());
            if (selectedMvz != null) {
                bean.setMvz(selectedMvz);
            } else {
                mvzItems.add(bean.getMvz());
            }
        }
        mvz.setItems(mvzItems);
        mvz.setItemLabelGenerator(m -> formatCodeName(m.getCode(), m.getName()));
        mvz.setWidthFull();
        mvz.setClearButtonVisible(true);

        binder.forField(cfo).bind(Request::getCfo2, Request::setCfo2);
        binder.forField(mvz).bind(Request::getMvz, Request::setMvz);

        ComboBox<Bdz> bdz = new ComboBox<>("БДЗ");
        List<Bdz> bdzItems = new ArrayList<>(bdzService.findAll());
        if (bean.getBdz() != null) {
            Bdz selectedBdz = findById(bdzItems, Bdz::getId, bean.getBdz().getId());
            if (selectedBdz != null) {
                bean.setBdz(selectedBdz);
            } else {
                bdzItems.add(bean.getBdz());
            }
        }
        bdz.setItems(bdzItems);
        bdz.setItemLabelGenerator(b -> formatCodeName(b.getCode(), b.getName()));
        bdz.setWidthFull();
        bdz.setClearButtonVisible(true);

        ComboBox<Bo> bo = new ComboBox<>("БО");
        bo.setItemLabelGenerator(b -> formatCodeName(b.getCode(), b.getName()));
        bo.setWidthFull();
        bo.setClearButtonVisible(true);

        TextField zgd = new TextField("ЗГД (автоподстановка)");
        zgd.setReadOnly(true);
        zgd.setWidthFull();

        bdz.addValueChangeListener(e -> {
            List<Bo> options = e.getValue() != null ? boRepository.findByBdzId(e.getValue().getId()) : List.of();
            bo.setItems(options);
            if (bo.getValue() != null) {
                Long currentId = bo.getValue().getId();
                boolean present = options.stream().anyMatch(item -> item.getId().equals(currentId));
                if (!present) {
                    bo.clear();
                }
            }
            Request current = binder.getBean();
            if (current != null) {
                current.setZgd(e.getValue() != null ? e.getValue().getZgd() : null);
            }
            zgd.setValue(e.getValue() != null && e.getValue().getZgd() != null ? e.getValue().getZgd().getFullName() : "");
        });

        binder.forField(bdz).bind(Request::getBdz, Request::setBdz);
        binder.forField(bo).bind(Request::getBo, Request::setBo);

        ComboBox<Contract> contract = new ComboBox<>("Договор");
        List<Contract> contractItems = new ArrayList<>(contractRepository.findAll());
        if (bean.getContract() != null) {
            Contract selectedContract = findById(contractItems, Contract::getId, bean.getContract().getId());
            if (selectedContract != null) {
                bean.setContract(selectedContract);
            } else {
                contractItems.add(bean.getContract());
            }
        }
        contract.setItems(contractItems);
        contract.setItemLabelGenerator(Contract::getName);
        contract.setWidthFull();
        contract.setClearButtonVisible(true);
        binder.forField(contract).bind(Request::getContract, Request::setContract);

        TextField vgo = new TextField("ВГО");
        vgo.setWidthFull();
        binder.bind(vgo, Request::getVgo, Request::setVgo);

        NumberField amount = new NumberField("Сумма (млн)");
        amount.setWidthFull();
        binder.forField(amount).bind(
                r -> r.getAmount() != null ? r.getAmount().doubleValue() : null,
                (r, v) -> r.setAmount(v != null ? BigDecimal.valueOf(v) : null));

        NumberField amountNoVat = new NumberField("Сумма без НДС (млн)");
        amountNoVat.setWidthFull();
        binder.forField(amountNoVat).bind(
                r -> r.getAmountNoVat() != null ? r.getAmountNoVat().doubleValue() : null,
                (r, v) -> r.setAmountNoVat(v != null ? BigDecimal.valueOf(v) : null));

        TextField subject = new TextField("Предмет договора");
        subject.setWidthFull();
        binder.bind(subject, Request::getSubject, Request::setSubject);

        ComboBox<String> period = new ComboBox<>("Период (месяц)");
        period.setItems(monthOptions());
        period.setAllowCustomValue(false);
        period.setClearButtonVisible(true);
        period.setWidthFull();
        binder.bind(period, Request::getPeriod, Request::setPeriod);

        TextField pm = new TextField("Способ закупки");
        pm.setWidthFull();
        binder.bind(pm, Request::getProcurementMethod, Request::setProcurementMethod);

        Checkbox input = new Checkbox("Вводный объект");
        input.setWidthFull();
        binder.bind(input, Request::isInputObject, Request::setInputObject);

        binder.setBean(bean);

        if (bean.getBdz() != null) {
            List<Bo> boItems = boRepository.findByBdzId(bean.getBdz().getId());
            bo.setItems(boItems);
            Bo selected = findById(boItems, Bo::getId, bean.getBo() != null ? bean.getBo().getId() : null);
            if (selected != null) {
                bo.setValue(selected);
            }
            Zgd currentZgd = bean.getZgd() != null ? bean.getZgd() : bean.getBdz().getZgd();
            if (currentZgd != null) {
                zgd.setValue(currentZgd.getFullName());
                binder.getBean().setZgd(currentZgd);
            } else {
                zgd.clear();
            }
        } else {
            bo.setItems(List.of());
            if (bean.getZgd() != null) {
                zgd.setValue(bean.getZgd().getFullName());
            } else {
                zgd.clear();
            }
        }

        Span stepIndicator = new Span("Шаг 1 из 4");
        VerticalLayout step1 = stepLayout(cfo, mvz);
        VerticalLayout step2 = stepLayout(bdz, bo, zgd);
        VerticalLayout step3 = stepLayout(contract);
        VerticalLayout step4 = stepLayout(vgo, amount, amountNoVat, subject, period, pm, input);
        step2.setVisible(false);
        step3.setVisible(false);
        step4.setVisible(false);

        Button back = new Button("Назад");
        Button next = new Button("Далее");
        back.setEnabled(false);

        back.addClickListener(e -> {
            if (step4.isVisible()) {
                step4.setVisible(false);
                step3.setVisible(true);
                stepIndicator.setText("Шаг 3 из 4");
                next.setEnabled(true);
            } else if (step3.isVisible()) {
                step3.setVisible(false);
                step2.setVisible(true);
                stepIndicator.setText("Шаг 2 из 4");
            } else if (step2.isVisible()) {
                step2.setVisible(false);
                step1.setVisible(true);
                stepIndicator.setText("Шаг 1 из 4");
                back.setEnabled(false);
            }
        });

        next.addClickListener(e -> {
            if (step1.isVisible()) {
                step1.setVisible(false);
                step2.setVisible(true);
                stepIndicator.setText("Шаг 2 из 4");
                back.setEnabled(true);
            } else if (step2.isVisible()) {
                step2.setVisible(false);
                step3.setVisible(true);
                stepIndicator.setText("Шаг 3 из 4");
            } else if (step3.isVisible()) {
                step3.setVisible(false);
                step4.setVisible(true);
                stepIndicator.setText("Шаг 4 из 4");
                next.setEnabled(false);
            }
        });

        Button save = new Button(editing ? "Сохранить изменения" : "Сохранить", e -> {
            Request current = binder.getBean();
            if (current == null) {
                return;
            }
            if (bdz.getValue() != null) {
                current.setZgd(bdz.getValue().getZgd());
            }
            requestService.save(current);
            d.close();
            reload();
        });
        Button close = new Button("Закрыть", e -> d.close());

        HorizontalLayout actions = new HorizontalLayout(back, next, save, close);
        actions.setPadding(false);
        actions.setSpacing(true);
        actions.setWidthFull();
        actions.getStyle().set("margin-top", "var(--lumo-space-m)");

        VerticalLayout layout = new VerticalLayout(stepIndicator, step1, step2, step3, step4, actions);
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setAlignItems(Alignment.STRETCH);
        layout.setWidthFull();
        layout.getStyle().set("max-width", "600px");
        layout.getStyle().set("margin", "0 auto");
        stepIndicator.getStyle().set("align-self", "flex-start");
        stepIndicator.getStyle().set("margin-bottom", "var(--lumo-space-m)");
        stepIndicator.getStyle().set("font-weight", "600");

        d.add(layout);
        d.open();
    }

    private com.vaadin.flow.component.Component infoSection(String title, InfoEntry... entries) {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(true);
        section.setSpacing(false);
        section.setWidthFull();
        section.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
        section.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        section.getStyle().set("padding", "var(--lumo-space-m)");
        section.getStyle().set("gap", "var(--lumo-space-s)");

        Span header = new Span(title);
        header.getStyle().set("font-weight", "600");
        header.getStyle().set("margin-bottom", "var(--lumo-space-s)");
        section.add(header);

        FormLayout layout = new FormLayout();
        layout.setWidthFull();
        layout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2)
        );

        for (InfoEntry entry : entries) {
            Span value = new Span(entry.value());
            value.getStyle().set("font-weight", "500");
            layout.addFormItem(value, entry.label());
        }

        section.add(layout);
        return section;
    }

    private VerticalLayout stepLayout(com.vaadin.flow.component.Component... components) {
        VerticalLayout layout = new VerticalLayout(components);
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setAlignItems(Alignment.STRETCH);
        layout.setWidthFull();
        layout.getStyle().set("row-gap", "var(--lumo-space-m)");
        layout.getStyle().set("margin-bottom", "var(--lumo-space-m)");
        return layout;
    }

    private <T> T findById(List<T> items, Function<T, Long> idExtractor, Long id) {
        if (id == null) {
            return null;
        }
        return items.stream()
                .filter(item -> id.equals(idExtractor.apply(item)))
                .findFirst()
                .orElse(null);
    }

    private String formatCodeName(String code, String name) {
        String codePart = code != null ? code.trim() : "";
        String namePart = name != null ? name.trim() : "";
        if (!codePart.isEmpty() && !namePart.isEmpty()) {
            return codePart + " — " + namePart;
        }
        if (!codePart.isEmpty()) {
            return codePart;
        }
        return namePart;
    }

    private InfoEntry entry(String label, String value) {
        return new InfoEntry(label, valueOrDash(value));
    }

    private InfoEntry entry(String label, BigDecimal value) {
        return new InfoEntry(label, valueOrDash(value));
    }

    private InfoEntry entry(String label, boolean value) {
        return new InfoEntry(label, yesNo(value));
    }

    private InfoEntry entry(String label, LocalDate value) {
        return new InfoEntry(label, valueOrDash(value));
    }

    private String valueOrDash(String value) {
        if (value == null || value.isBlank()) {
            return "—";
        }
        return value;
    }

    private String valueOrDash(BigDecimal value) {
        return value != null ? value.toPlainString() : "—";
    }

    private String valueOrDash(LocalDate value) {
        return value != null ? DATE_FORMATTER.format(value) : "—";
    }

    private String yesNo(boolean value) {
        return value ? "Да" : "Нет";
    }

    private List<String> monthOptions() {
        Locale locale = new Locale("ru");
        return Arrays.stream(Month.values())
                .map(month -> capitalize(month.getDisplayName(TextStyle.FULL_STANDALONE, locale), locale))
                .collect(Collectors.toList());
    }

    private String capitalize(String value, Locale locale) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return value.substring(0, 1).toUpperCase(locale) + value.substring(1);
    }
    private record InfoEntry(String label, String value) {}
}
