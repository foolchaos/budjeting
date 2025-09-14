package com.example.budjeting.ui;

import com.example.budjeting.entity.Request;
import com.example.budjeting.repository.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Заявки")
@Route(value = "requests", layout = MainLayout.class)
public class RequestsView extends VerticalLayout {
    private final RequestRepository requestRepository;
    private final BdzRepository bdzRepository;
    private final BoRepository boRepository;
    private final CfoRepository cfoRepository;
    private final MvzRepository mvzRepository;

    private final Grid<Request> grid = new Grid<>(Request.class, false);

    public RequestsView(RequestRepository requestRepository, BdzRepository bdzRepository, BoRepository boRepository,
                        CfoRepository cfoRepository, MvzRepository mvzRepository) {
        this.requestRepository = requestRepository;
        this.bdzRepository = bdzRepository;
        this.boRepository = boRepository;
        this.cfoRepository = cfoRepository;
        this.mvzRepository = mvzRepository;
        setSizeFull();

        grid.addColumn(Request::getNumber).setHeader("Номер");
        grid.addColumn(r -> r.getBdz() != null ? r.getBdz().getName() : "").setHeader("БДЗ");
        grid.addColumn(r -> r.getCfo() != null ? r.getCfo().getName() : "").setHeader("ЦФО");
        grid.addColumn(r -> r.getMvz() != null ? r.getMvz().getName() : "").setHeader("МВЗ");
        grid.addColumn(r -> r.getBo() != null ? r.getBo().getName() : "").setHeader("БО");
        grid.addColumn(Request::getVgo).setHeader("ВГО");
        grid.addColumn(Request::getAmount).setHeader("Сумма");
        grid.addColumn(Request::getAmountWithoutVat).setHeader("Сумма без НДС");
        grid.addColumn(Request::getSubject).setHeader("Предмет");
        grid.addColumn(Request::getPeriod).setHeader("Период");
        grid.addColumn(Request::isIntroductory).setHeader("Вводный объект");
        grid.addColumn(Request::getProcurementMethod).setHeader("Способ закупки");
        grid.setSizeFull();

        Button add = new Button("Новая заявка", e -> openForm(new Request()));

        add(add, grid);
        expand(grid);
        refresh();
    }

    private void openForm(Request request) {
        RequestForm form = new RequestForm(bdzRepository, boRepository, cfoRepository, mvzRepository, this::saveRequest);
        form.setRequest(request);
        form.open();
    }

    private void saveRequest(Request request) {
        requestRepository.save(request);
        refresh();
    }

    private void refresh() {
        grid.setItems(requestRepository.findAll());
    }
}
