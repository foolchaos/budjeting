package com.example.budget.ui;

import com.example.budget.domain.*;
import com.example.budget.repo.BoRepository;
import com.example.budget.repo.CfoRepository;
import com.example.budget.repo.CfoTwoRepository;
import com.example.budget.repo.ContractRepository;
import com.example.budget.repo.CounterpartyRepository;
import com.example.budget.repo.MvzRepository;
import com.example.budget.service.BdzService;
import com.example.budget.service.RequestExcelExportResult;
import com.example.budget.service.RequestExcelExportService;
import com.example.budget.service.RequestPositionService;
import com.example.budget.service.RequestService;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@UIScope
@CssImport("./styles/views/requests-view.css")
public class RequestsView extends VerticalLayout {

    private final RequestService requestService;
    private final RequestPositionService requestPositionService;
    private final BdzService bdzService;
    private final BoRepository boRepository;
    private final CfoRepository cfoRepository;
    private final CfoTwoRepository cfoTwoRepository;
    private final MvzRepository mvzRepository;
    private final ContractRepository contractRepository;
    private final CounterpartyRepository counterpartyRepository;
    private final RequestExcelExportService requestExcelExportService;

    private final Grid<Request> requestsGrid = new Grid<>(Request.class, false);
    private final List<Request> requests = new ArrayList<>();
    private final ListDataProvider<Request> requestsDataProvider = new ListDataProvider<>(requests);
    private final Set<Long> checkedRequestIds = new HashSet<>();
    private final Checkbox selectAllRequests = new Checkbox();

    private final Button deleteRequestButton = new Button("Удалить выбранные");
    private final Button exportRequestButton = new Button("Экспорт");
    private Grid.Column<Request> requestSelectionColumn;

    private final Grid<RequestPosition> grid = new Grid<>(RequestPosition.class, false);
    private final List<RequestPosition> allPositions = new ArrayList<>();
    private final Select<Integer> pageSizeSelect = new Select<>();
    private final Button prevPage = new Button("Назад");
    private final Button nextPage = new Button("Вперёд");
    private final Button createPositionButton = new Button("Создать позицию");
    private final Button deletePositionButton = new Button("Удалить выбранные");
    private final Span pageInfo = new Span();
    private int currentPage = 0;
    private Request selectedRequest;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final int MIN_REQUEST_YEAR = 2000;
    private static final int MAX_REQUEST_YEAR = 2050;

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestsView.class);

    public RequestsView(RequestService requestService, RequestPositionService requestPositionService,
                        BdzService bdzService, BoRepository boRepository, CfoRepository cfoRepository,
                        CfoTwoRepository cfoTwoRepository,
                        MvzRepository mvzRepository, ContractRepository contractRepository,
                        CounterpartyRepository counterpartyRepository,
                        RequestExcelExportService requestExcelExportService) {
        this.requestService = requestService;
        this.requestPositionService = requestPositionService;
        this.bdzService = bdzService;
        this.boRepository = boRepository;
        this.cfoRepository = cfoRepository;
        this.cfoTwoRepository = cfoTwoRepository;
        this.mvzRepository = mvzRepository;
        this.contractRepository = contractRepository;
        this.counterpartyRepository = counterpartyRepository;
        this.requestExcelExportService = requestExcelExportService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        exportRequestButton.addClickListener(e -> exportSelectedRequest());
        exportRequestButton.setEnabled(false);

        configureSelectAllRequestsCheckbox();

        SplitLayout splitLayout = new SplitLayout();
        splitLayout.setSizeFull();
        splitLayout.setSplitterPosition(30);
        splitLayout.addToPrimary(buildRequestsPanel());
        splitLayout.addToSecondary(buildPositionsPanel());

        add(splitLayout);
        setFlexGrow(1, splitLayout);

        reloadRequests();
    }

    private VerticalLayout buildRequestsPanel() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setPadding(true);
        layout.setSpacing(false);
        layout.setAlignItems(Alignment.STRETCH);

        requestsGrid.setDataProvider(requestsDataProvider);
        requestsGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
        requestsGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        requestsGrid.addClassName("requests-grid");
        requestsGrid.setWidthFull();

        requestSelectionColumn = requestsGrid.addColumn(createRequestSelectionRenderer());
        requestSelectionColumn.setHeader(selectAllRequests);
        requestSelectionColumn.setTextAlign(ColumnTextAlign.CENTER);
        requestSelectionColumn.setFlexGrow(0);
        requestSelectionColumn.setAutoWidth(true);
        requestSelectionColumn.setKey("request-select");
        requestSelectionColumn.setWidth("3.5em");

        requestsGrid.addColumn(Request::getName)
                .setHeader("Заявка")
                .setAutoWidth(true)
                .setFlexGrow(1);
        requestsGrid.addColumn(r -> valueOrDash(r.getCfo() != null ? r.getCfo().getCode() : null))
                .setHeader("ЦФО I")
                .setAutoWidth(true)
                .setFlexGrow(1);
        requestsGrid.addColumn(r -> valueOrDash(r.getYear()))
                .setHeader("Год")
                .setAutoWidth(true)
                .setFlexGrow(0);
        requestsGrid.addColumn(r -> r.getPositions() != null ? r.getPositions().size() : 0)
                .setHeader("Позиций")
                .setAutoWidth(true)
                .setFlexGrow(0);

        requestsGrid.addItemClickListener(event -> {
            if (event.getColumn() != null && event.getColumn() == requestSelectionColumn) {
                return;
            }
            requestsGrid.select(event.getItem());
        });
        requestsGrid.addItemDoubleClickListener(event -> {
            if (event.getColumn() != null && event.getColumn() == requestSelectionColumn) {
                return;
            }
            requestsGrid.select(event.getItem());
            openRequestCard(event.getItem());
        });
        requestsGrid.addSelectionListener(event ->
                handleRequestSelection(event.getFirstSelectedItem().orElse(null)));
        requestsGrid.setSizeFull();

        Button create = new Button("Создать заявку", e -> openRequestForm(new Request(), false));
        create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        deleteRequestButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteRequestButton.setEnabled(false);
        deleteRequestButton.addClickListener(e -> {
            List<Long> toDelete = new ArrayList<>(checkedRequestIds);
            toDelete.stream()
                    .filter(Objects::nonNull)
                    .forEach(requestService::deleteById);
            checkedRequestIds.clear();
            reloadRequests();
        });

        HorizontalLayout leftActions = new HorizontalLayout(create, deleteRequestButton);
        leftActions.setSpacing(true);
        leftActions.setPadding(false);
        leftActions.setMargin(false);
        leftActions.setAlignItems(Alignment.CENTER);

        HorizontalLayout actions = new HorizontalLayout(leftActions, exportRequestButton);
        actions.setWidthFull();
        actions.setSpacing(true);
        actions.setPadding(false);
        actions.setMargin(false);
        actions.setAlignItems(Alignment.CENTER);
        actions.setJustifyContentMode(JustifyContentMode.BETWEEN);

        layout.add(actions, requestsGrid);
        layout.setFlexGrow(1, requestsGrid);
        return layout;
    }

    private VerticalLayout buildPositionsPanel() {
        configurePositionsGrid();

        createPositionButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createPositionButton.addClickListener(e -> openWizard());
        createPositionButton.setEnabled(false);

        deletePositionButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deletePositionButton.setEnabled(false);
        deletePositionButton.addClickListener(e -> {
            List<RequestPosition> selected = new ArrayList<>(grid.getSelectedItems());
            selected.forEach(position -> requestPositionService.deleteById(position.getId()));
            reloadRequests();
        });

        grid.addSelectionListener(e -> updatePositionButtons());
        grid.setSizeFull();

        HorizontalLayout actions = new HorizontalLayout(createPositionButton, deletePositionButton);
        actions.setWidthFull();

        HorizontalLayout pagination = buildPaginationBar();

        VerticalLayout layout = new VerticalLayout(actions, grid, pagination);
        layout.setSizeFull();
        layout.setPadding(true);
        layout.setSpacing(false);
        layout.setAlignItems(Alignment.STRETCH);
        layout.setFlexGrow(1, grid);
        layout.setFlexGrow(0, pagination);
        return layout;
    }

    private void reloadRequests() {
        Long selectedId = selectedRequest != null ? selectedRequest.getId() : null;
        requests.clear();
        requests.addAll(requestService.findAll());
        Set<Long> existingIds = requests.stream()
                .map(Request::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        checkedRequestIds.retainAll(existingIds);
        requestsDataProvider.refreshAll();
        updateRequestActionButtons();
        updateSelectAllCheckboxState();

        if (selectedId != null) {
            Request match = requests.stream()
                    .filter(r -> selectedId.equals(r.getId()))
                    .findFirst()
                    .orElse(null);
            if (match != null) {
                requestsGrid.select(match);
                return;
            }
        }

        requestsGrid.deselectAll();
        handleRequestSelection(null);
    }

    private void handleRequestSelection(Request request) {
        if (!Objects.equals(selectedRequest, request)) {
            selectedRequest = request;
            requestsDataProvider.refreshAll();
            reloadPositions();
        } else {
            updatePositionButtons();
        }
    }

    private ComponentRenderer<Checkbox, Request> createRequestSelectionRenderer() {
        return new ComponentRenderer<>(() -> {
            Checkbox checkbox = new Checkbox();
            checkbox.addValueChangeListener(event -> {
                if (!event.isFromClient()) {
                    return;
                }
                Request current = ComponentUtil.getData(checkbox, Request.class);
                if (current == null || current.getId() == null) {
                    return;
                }
                if (Boolean.TRUE.equals(event.getValue())) {
                    checkedRequestIds.add(current.getId());
                } else {
                    checkedRequestIds.remove(current.getId());
                }
                updateRequestActionButtons();
                updateSelectAllCheckboxState();
            });
            checkbox.getElement().executeJs(
                    "this.addEventListener('click', function(event) { event.stopPropagation(); });");
            return checkbox;
        }, (checkbox, request) -> {
            ComponentUtil.setData(checkbox, Request.class, request);
            boolean checked = request != null && request.getId() != null
                    && checkedRequestIds.contains(request.getId());
            checkbox.setValue(checked);
            checkbox.setEnabled(request != null && request.getId() != null);
        });
    }

    private void updateRequestActionButtons() {
        deleteRequestButton.setEnabled(!checkedRequestIds.isEmpty());
        exportRequestButton.setEnabled(checkedRequestIds.size() == 1);
    }

    private void exportSelectedRequest() {
        if (checkedRequestIds.size() != 1) {
            Notification.show("Выберите одну заявку для экспорта");
            return;
        }
        Long requestId = checkedRequestIds.iterator().next();
        try {
            RequestExcelExportResult result = requestExcelExportService.exportRequest(requestId);
            StreamResource resource = new StreamResource(result.fileName(),
                    () -> new ByteArrayInputStream(result.content()));
            resource.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            resource.setCacheTime(0);
            StreamRegistration registration = VaadinSession.getCurrent().getResourceRegistry().registerResource(resource);
            getUI().ifPresent(ui -> ui.getPage().open(registration.getResourceUri().toString()));
        } catch (Exception ex) {
            LOGGER.error("Failed to export request {}", requestId, ex);
            Notification.show("Не удалось экспортировать заявку");
        }
    }

    private void configureSelectAllRequestsCheckbox() {
        selectAllRequests.addValueChangeListener(event -> {
            if (!event.isFromClient()) {
                return;
            }
            boolean selectAll = Boolean.TRUE.equals(event.getValue());
            if (selectAll) {
                checkedRequestIds.clear();
                requests.stream()
                        .map(Request::getId)
                        .filter(Objects::nonNull)
                        .forEach(checkedRequestIds::add);
            } else {
                checkedRequestIds.clear();
            }
            requestsDataProvider.refreshAll();
            updateRequestActionButtons();
            updateSelectAllCheckboxState();
        });
        selectAllRequests.getElement().getStyle().set("margin", "0 auto");
        selectAllRequests.getElement().setProperty("title", "Выбрать или снять выделение со всех заявок");
    }

    private void updateSelectAllCheckboxState() {
        long totalSelectable = requests.stream()
                .map(Request::getId)
                .filter(Objects::nonNull)
                .count();

        boolean enableHeaderCheckbox = totalSelectable > 0;
        selectAllRequests.setEnabled(enableHeaderCheckbox);

        if (!enableHeaderCheckbox) {
            if (selectAllRequests.getValue()) {
                selectAllRequests.setValue(false);
            }
            if (selectAllRequests.isIndeterminate()) {
                selectAllRequests.setIndeterminate(false);
            }
            return;
        }

        int selectedCount = checkedRequestIds.size();
        if (selectedCount == 0) {
            if (selectAllRequests.getValue()) {
                selectAllRequests.setValue(false);
            }
            if (selectAllRequests.isIndeterminate()) {
                selectAllRequests.setIndeterminate(false);
            }
        } else if (selectedCount == totalSelectable) {
            if (!selectAllRequests.getValue()) {
                selectAllRequests.setValue(true);
            }
            if (selectAllRequests.isIndeterminate()) {
                selectAllRequests.setIndeterminate(false);
            }
        } else {
            if (selectAllRequests.getValue()) {
                selectAllRequests.setValue(false);
            }
            if (!selectAllRequests.isIndeterminate()) {
                selectAllRequests.setIndeterminate(true);
            }
        }
    }

    private void updatePositionButtons() {
        boolean hasRequest = selectedRequest != null && selectedRequest.getId() != null;
        createPositionButton.setEnabled(hasRequest);
        deletePositionButton.setEnabled(hasRequest && !grid.getSelectedItems().isEmpty());
    }

    private void openRequestCard(Request entity) {
        Request detailed = requestService.findById(entity.getId());
        Dialog dialog = new Dialog("Заявка: " + valueOrDash(detailed.getName()));
        dialog.setWidth("420px");

        FormLayout form = new FormLayout();
        form.setWidthFull();
        form.addFormItem(new Span(valueOrDash(detailed.getName())), "Наименование");
        form.addFormItem(new Span(valueOrDash(detailed.getYear())), "Год");
        form.addFormItem(new Span(valueOrDash(formatCodeName(detailed.getCfo()))), "ЦФО I");
        int count = detailed.getPositions() != null ? detailed.getPositions().size() : 0;
        form.addFormItem(new Span(String.valueOf(count)), "Количество позиций");

        VerticalLayout content = new VerticalLayout(form);
        content.setPadding(false);
        content.setSpacing(false);
        content.setWidthFull();

        Button edit = new Button("Редактировать", e -> {
            dialog.close();
            openRequestForm(detailed, true);
        });
        Button delete = new Button("Удалить", e -> {
            requestService.deleteById(detailed.getId());
            checkedRequestIds.remove(detailed.getId());
            dialog.close();
            reloadRequests();
        });
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        Button close = new Button("Закрыть", e -> dialog.close());

        HorizontalLayout actions = new HorizontalLayout(edit, delete, close);
        actions.setWidthFull();
        actions.setJustifyContentMode(JustifyContentMode.END);

        dialog.add(content, actions);
        dialog.open();
    }

    private void openRequestForm(Request bean, boolean editing) {
        Request target = editing && bean.getId() != null ? requestService.findById(bean.getId()) : bean;

        Dialog dialog = new Dialog(editing ? "Редактирование заявки" : "Создание заявки");
        dialog.setWidth("420px");

        Binder<Request> binder = new Binder<>(Request.class);
        TextField name = new TextField("Наименование");
        name.setWidthFull();
        List<Integer> yearOptions = new ArrayList<>(requestYearOptions());
        Integer targetYear = target.getYear();
        if (targetYear != null && !yearOptions.contains(targetYear)) {
            yearOptions.add(targetYear);
            yearOptions.sort(Integer::compareTo);
        }
        ComboBox<Integer> year = new ComboBox<>("Год");
        year.setWidthFull();
        year.setItems(yearOptions);
        year.setItemLabelGenerator(String::valueOf);
        year.setAllowCustomValue(false);
        year.setRequiredIndicatorVisible(true);

        ComboBox<Cfo> cfoField = new ComboBox<>("ЦФО I");
        List<Cfo> cfoOptions = new ArrayList<>(cfoRepository.findAll());
        if (target.getCfo() != null) {
            Cfo selectedCfo = findById(cfoOptions, Cfo::getId, target.getCfo().getId());
            if (selectedCfo != null) {
                target.setCfo(selectedCfo);
            } else {
                cfoOptions.add(target.getCfo());
            }
        }
        cfoField.setItems(cfoOptions);
        cfoField.setItemLabelGenerator(this::formatCodeName);
        cfoField.setWidthFull();
        cfoField.setClearButtonVisible(true);
        cfoField.setRequiredIndicatorVisible(true);

        binder.forField(name)
                .asRequired("Введите наименование")
                .bind(Request::getName, Request::setName);
        binder.forField(year)
                .asRequired("Выберите год")
                .bind(Request::getYear, Request::setYear);
        binder.forField(cfoField)
                .asRequired("Выберите ЦФО I")
                .bind(Request::getCfo, Request::setCfo);
        binder.setBean(target);

        if (year.getValue() == null) {
            int currentYear = LocalDate.now().getYear();
            Integer defaultYear = null;
            if (currentYear >= MIN_REQUEST_YEAR && currentYear <= MAX_REQUEST_YEAR) {
                defaultYear = currentYear;
            } else if (!yearOptions.isEmpty()) {
                defaultYear = yearOptions.get(0);
            }
            if (defaultYear != null) {
                year.setValue(defaultYear);
            }
        }

        Button save = new Button("Сохранить", e -> {
            if (binder.validate().isOk()) {
                Request saved = requestService.save(target);
                selectedRequest = saved;
                dialog.close();
                reloadRequests();
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button("Отмена", e -> dialog.close());

        HorizontalLayout actions = new HorizontalLayout(save, cancel);
        actions.setWidthFull();
        actions.setJustifyContentMode(JustifyContentMode.END);

        VerticalLayout content = new VerticalLayout(name, year, cfoField);
        content.setPadding(false);
        content.setSpacing(true);
        content.setWidthFull();

        dialog.add(content, actions);
        dialog.open();
    }

    private void configurePositionsGrid() {
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setWidthFull();
        grid.addColumn(RequestPosition::getNumber)
                .setHeader("Номер")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(r -> valueOrDash(r.getCfo2() != null ? r.getCfo2().getCode() : null))
                .setHeader("ЦФО II")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(r -> valueOrDash(r.getMvz() != null ? r.getMvz().getCode() : null))
                .setHeader("МВЗ")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(r -> {
                    Bdz bdz = r.getBdz();
                    return bdz != null ? formatCodeName(bdz.getCode(), bdz.getName()) : "—";
                })
                .setHeader("БДЗ")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(r -> {
                    Bo bo = r.getBo();
                    return bo != null ? formatCodeName(bo.getCode(), bo.getName()) : "—";
                })
                .setHeader("БО")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(r -> formatZgd(r.getZgd()))
                .setHeader("ЗГД")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(r -> valueOrDash(r.getVgo()))
                .setHeader("ВГО")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(r -> {
                    Counterparty counterparty = r.getCounterparty();
                    return counterparty != null ? valueOrDash(counterparty.getLegalEntityName()) : "—";
                })
                .setHeader("Контрагент")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(r -> {
                    Contract contract = r.getContract();
                    return contract != null ? valueOrDash(contract.getName()) : "—";
                })
                .setHeader("Договор")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(r -> {
                    Contract contract = r.getContract();
                    return contract != null ? valueOrDash(contract.getInternalNumber()) : "—";
                })
                .setHeader("№ договора (внутренний)")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(r -> {
                    Contract contract = r.getContract();
                    return contract != null ? valueOrDash(contract.getExternalNumber()) : "—";
                })
                .setHeader("№ договора (внешний)")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(r -> {
                    Contract contract = r.getContract();
                    return contract != null ? valueOrDash(contract.getContractDate()) : "—";
                })
                .setHeader("Дата договора")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(r -> {
                    Contract contract = r.getContract();
                    return contract != null ? valueOrDash(contract.getResponsible()) : "—";
                })
                .setHeader("Ответственный по договору (Ф.И.О.)")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(r -> valueOrDash(r.getProcurementMethod()))
                .setHeader("Способ закупки")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(r -> valueOrDash(r.getPeriod()))
                .setHeader("Период")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(r -> valueOrDash(r.getAmountNoVat()))
                .setHeader("Сумма/млн. руб. (без НДС)")
                .setAutoWidth(true)
                .setFlexGrow(1)
                .setTextAlign(ColumnTextAlign.END);
        grid.addColumn(r -> yesNo(r.isInputObject()))
                .setHeader("Вводный объект")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addItemClickListener(e -> openPositionCard(e.getItem()));
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
            if ((currentPage + 1) * pageSizeSelect.getValue() < allPositions.size()) {
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

    private void reloadPositions() {
        allPositions.clear();
        if (selectedRequest != null && selectedRequest.getId() != null) {
            allPositions.addAll(requestPositionService.findByRequestId(selectedRequest.getId()));
        }
        currentPage = 0;
        grid.deselectAll();
        updateGridPage();
        updatePositionButtons();
    }

    private void updateGridPage() {
        Integer selectedSize = pageSizeSelect.getValue();
        int pageSize = selectedSize != null ? selectedSize : 10;
        int total = allPositions.size();

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

        grid.setItems(allPositions.subList(fromIndex, toIndex));
        pageInfo.setText(String.format("%d–%d из %d", fromIndex + 1, toIndex, total));

        prevPage.setEnabled(currentPage > 0);
        nextPage.setEnabled(currentPage < pageCount - 1);
    }

    private void openPositionCard(RequestPosition entity) {
        RequestPosition detailed = requestPositionService.findDetailedById(entity.getId());
        Dialog d = new Dialog("Позиция заявки № " + detailed.getNumber());
        d.setWidth("900px");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);
        content.setWidthFull();
        content.getStyle().set("max-height", "70vh");
        content.getStyle().set("overflow", "auto");

        content.add(
                infoSection("Основная информация",
                        entry("Заявка", detailed.getRequest() != null ? detailed.getRequest().getName() : null),
                        entry("Номер позиции", detailed.getNumber()),
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
                        entry("Контрагент", detailed.getCounterparty() != null ? detailed.getCounterparty().getLegalEntityName() : null),
                        entry("Наименование", detailed.getContract() != null ? detailed.getContract().getName() : null),
                        entry("Внутренний номер", detailed.getContract() != null ? detailed.getContract().getInternalNumber() : null),
                        entry("Внешний номер", detailed.getContract() != null ? detailed.getContract().getExternalNumber() : null),
                        entry("Дата договора", detailed.getContract() != null ? detailed.getContract().getContractDate() : null),
                        entry("Ответственный", detailed.getContract() != null ? detailed.getContract().getResponsible() : null)
                )
        );

        Button edit = new Button("Редактировать", e -> {
            d.close();
            openPositionEdit(entity);
        });
        Button del = new Button("Удалить", e -> {
            requestPositionService.deleteById(detailed.getId());
            d.close();
            reloadRequests();
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

    private void openPositionEdit(RequestPosition entity) {
        openWizard(requestPositionService.findDetailedById(entity.getId()), true);
    }

    private void openWizard() {
        if (selectedRequest == null) {
            return;
        }
        RequestPosition bean = new RequestPosition();
        bean.setRequest(selectedRequest);
        openWizard(bean, false);
    }

    private void openWizard(RequestPosition bean, boolean editing) {
        Dialog d = new Dialog(editing ? "Редактирование позиции № " + bean.getNumber() : "Создание позиции заявки");
        d.setWidth("600px");

        Binder<RequestPosition> binder = new Binder<>(RequestPosition.class);

        TextField requestName = new TextField("Заявка");
        requestName.setWidthFull();
        requestName.setReadOnly(true);
        requestName.setValue(bean.getRequest() != null ? valueOrDash(bean.getRequest().getName()) : "");

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
        Request requestSource = bean.getRequest() != null ? bean.getRequest() : selectedRequest;
        Long cfoId = requestSource != null && requestSource.getCfo() != null
                ? requestSource.getCfo().getId()
                : null;
        List<Mvz> mvzItems = cfoId != null
                ? new ArrayList<>(mvzRepository.findByCfoId(cfoId))
                : new ArrayList<>();
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

        binder.forField(cfo).bind(RequestPosition::getCfo2, RequestPosition::setCfo2);
        binder.forField(mvz).bind(RequestPosition::getMvz, RequestPosition::setMvz);

        ComboBox<Bdz> bdz = new ComboBox<>("БДЗ");
        List<Bdz> bdzItems = cfoId != null
                ? new ArrayList<>(bdzService.findByCfoId(cfoId))
                : new ArrayList<>();
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
            RequestPosition current = binder.getBean();
            if (current != null) {
                current.setZgd(e.getValue() != null ? e.getValue().getZgd() : null);
            }
            zgd.setValue(e.getValue() != null && e.getValue().getZgd() != null ? e.getValue().getZgd().getFullName() : "");
        });

        binder.forField(bdz).bind(RequestPosition::getBdz, RequestPosition::setBdz);
        binder.forField(bo).bind(RequestPosition::getBo, RequestPosition::setBo);

        ComboBox<Counterparty> counterparty = new ComboBox<>("Контрагент");
        List<Counterparty> counterpartyItems = new ArrayList<>(counterpartyRepository.findAll());
        if (bean.getCounterparty() != null) {
            Counterparty selectedCounterparty = findById(counterpartyItems, Counterparty::getId,
                    bean.getCounterparty().getId());
            if (selectedCounterparty != null) {
                bean.setCounterparty(selectedCounterparty);
            } else {
                counterpartyItems.add(bean.getCounterparty());
            }
        }
        counterparty.setItems(counterpartyItems);
        counterparty.setItemLabelGenerator(Counterparty::getLegalEntityName);
        counterparty.setWidthFull();
        counterparty.setClearButtonVisible(true);

        ComboBox<Contract> contract = new ComboBox<>("Договор");
        List<Contract> allContracts = new ArrayList<>(contractRepository.findAll());
        if (bean.getContract() != null) {
            Contract selectedContract = findById(allContracts, Contract::getId, bean.getContract().getId());
            if (selectedContract != null) {
                bean.setContract(selectedContract);
            } else {
                allContracts.add(bean.getContract());
            }
        }
        contract.setItemLabelGenerator(Contract::getName);
        contract.setWidthFull();
        contract.setClearButtonVisible(true);
        contract.setEnabled(false);

        binder.forField(counterparty).bind(RequestPosition::getCounterparty, RequestPosition::setCounterparty);
        binder.forField(contract).bind(RequestPosition::getContract, RequestPosition::setContract);

        TextField vgo = new TextField("ВГО");
        vgo.setWidthFull();
        binder.bind(vgo, RequestPosition::getVgo, RequestPosition::setVgo);

        NumberField amountNoVat = new NumberField("Сумма без НДС (млн)");
        amountNoVat.setWidthFull();
        binder.forField(amountNoVat).bind(
                r -> r.getAmountNoVat() != null ? r.getAmountNoVat().doubleValue() : null,
                (r, v) -> r.setAmountNoVat(v != null ? BigDecimal.valueOf(v) : null));

        TextField subject = new TextField("Предмет договора");
        subject.setWidthFull();
        binder.bind(subject, RequestPosition::getSubject, RequestPosition::setSubject);

        ComboBox<String> period = new ComboBox<>("Период (месяц)");
        period.setItems(monthOptions());
        period.setAllowCustomValue(false);
        period.setClearButtonVisible(true);
        period.setWidthFull();
        binder.bind(period, RequestPosition::getPeriod, RequestPosition::setPeriod);

        TextField pm = new TextField("Способ закупки");
        pm.setWidthFull();
        binder.bind(pm, RequestPosition::getProcurementMethod, RequestPosition::setProcurementMethod);

        Checkbox input = new Checkbox("Вводный объект");
        input.setWidthFull();
        binder.bind(input, RequestPosition::isInputObject, RequestPosition::setInputObject);

        Runnable updateContractItems = () -> {
            Counterparty selected = counterparty.getValue();
            Contract currentContract = contract.getValue();
            if (selected == null || selected.getId() == null) {
                contract.setItems(List.of());
                if (currentContract != null) {
                    contract.clear();
                }
                contract.setEnabled(false);
                return;
            }

            List<Contract> filtered = allContracts.stream()
                    .filter(item -> item.getCounterparty() != null && item.getCounterparty().getId() != null
                            && item.getCounterparty().getId().equals(selected.getId()))
                    .collect(Collectors.toCollection(ArrayList::new));

            if (currentContract != null) {
                boolean present = filtered.stream().anyMatch(item -> item.getId().equals(currentContract.getId()));
                if (!present && currentContract.getCounterparty() != null
                        && currentContract.getCounterparty().getId() != null
                        && currentContract.getCounterparty().getId().equals(selected.getId())) {
                    filtered.add(currentContract);
                }
            }

            contract.setItems(filtered);

            if (currentContract != null && currentContract.getCounterparty() != null
                    && currentContract.getCounterparty().getId() != null
                    && !currentContract.getCounterparty().getId().equals(selected.getId())) {
                contract.clear();
            }

            contract.setEnabled(true);
        };

        counterparty.addValueChangeListener(e -> {
            updateContractItems.run();
            if (e.getValue() == null) {
                contract.clear();
            }
        });

        contract.addValueChangeListener(e -> {
            Contract selectedContract = e.getValue();
            if (selectedContract != null && selectedContract.getCounterparty() != null) {
                Counterparty contractCounterparty = selectedContract.getCounterparty();
                if (contractCounterparty.getId() != null) {
                    boolean present = counterpartyItems.stream()
                            .anyMatch(item -> item.getId().equals(contractCounterparty.getId()));
                    if (!present) {
                        counterpartyItems.add(contractCounterparty);
                        counterparty.setItems(counterpartyItems);
                    }
                }
                if (counterparty.getValue() == null || contractCounterparty.getId() != null
                        && !contractCounterparty.getId().equals(counterparty.getValue().getId())) {
                    counterparty.setValue(contractCounterparty);
                }
            }
        });

        binder.setBean(bean);

        if (contract.getValue() != null && contract.getValue().getCounterparty() != null) {
            Counterparty contractCounterparty = contract.getValue().getCounterparty();
            if (contractCounterparty.getId() != null) {
                boolean present = counterpartyItems.stream()
                        .anyMatch(item -> item.getId().equals(contractCounterparty.getId()));
                if (!present) {
                    counterpartyItems.add(contractCounterparty);
                    counterparty.setItems(counterpartyItems);
                }
            }
            if (counterparty.getValue() == null) {
                counterparty.setValue(contractCounterparty);
            }
        }

        updateContractItems.run();
        contract.setEnabled(counterparty.getValue() != null);

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
        VerticalLayout step1 = stepLayout(requestName, cfo, mvz);
        VerticalLayout step2 = stepLayout(bdz, bo, zgd);
        VerticalLayout step3 = stepLayout(counterparty, contract);
        VerticalLayout step4 = stepLayout(vgo, amountNoVat, subject, period, pm, input);
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
            RequestPosition current = binder.getBean();
            if (current == null) {
                return;
            }
            if (bdz.getValue() != null) {
                current.setZgd(bdz.getValue().getZgd());
            }
            if (current.getRequest() == null && selectedRequest != null) {
                current.setRequest(selectedRequest);
            }
            RequestPosition saved = requestPositionService.save(current);
            if (saved.getRequest() != null) {
                selectedRequest = saved.getRequest();
            }
            d.close();
            reloadRequests();
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

    private String formatCodeName(Cfo cfo) {
        return cfo != null ? formatCodeName(cfo.getCode(), cfo.getName()) : null;
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

    private String formatZgd(Zgd zgd) {
        if (zgd == null) {
            return "—";
        }
        String fullName = zgd.getFullName() != null ? zgd.getFullName().trim() : "";
        String department = zgd.getDepartment() != null ? zgd.getDepartment().trim() : "";
        if (!fullName.isEmpty() && !department.isEmpty()) {
            return fullName + " — " + department;
        }
        if (!fullName.isEmpty()) {
            return fullName;
        }
        if (!department.isEmpty()) {
            return department;
        }
        return "—";
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

    private String valueOrDash(Integer value) {
        return value != null ? value.toString() : "—";
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

    private List<Integer> requestYearOptions() {
        return IntStream.rangeClosed(MIN_REQUEST_YEAR, MAX_REQUEST_YEAR)
                .boxed()
                .collect(Collectors.toList());
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
