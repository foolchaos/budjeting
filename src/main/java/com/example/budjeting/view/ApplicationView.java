package com.example.budjeting.view;

import com.example.budjeting.entity.*;
import com.example.budjeting.repository.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "applications", layout = MainLayout.class)
@PageTitle("Заявки")
public class ApplicationView extends VerticalLayout {

    private final ApplicationRepository repository;
    private final BdzItemRepository bdzRepository;
    private final CfoRepository cfoRepository;
    private final MvzRepository mvzRepository;
    private final BoArticleRepository boRepository;
    private final ContractRepository contractRepository;

    private final Grid<Application> grid = new Grid<>(Application.class);
    private final Binder<Application> binder = new Binder<>(Application.class);
    private Application current;

    private ComboBox<Mvz> mvzCombo;
    private ComboBox<BoArticle> boCombo;

    public ApplicationView(@Autowired ApplicationRepository repository,
                           @Autowired BdzItemRepository bdzRepository,
                           @Autowired CfoRepository cfoRepository,
                           @Autowired MvzRepository mvzRepository,
                           @Autowired BoArticleRepository boRepository,
                           @Autowired ContractRepository contractRepository) {
        this.repository = repository;
        this.bdzRepository = bdzRepository;
        this.cfoRepository = cfoRepository;
        this.mvzRepository = mvzRepository;
        this.boRepository = boRepository;
        this.contractRepository = contractRepository;
        configureGrid();
        add(grid, createForm());
        updateList();
    }

    private void configureGrid() {
        grid.setColumns("number", "bdzItem", "cfo", "mvz", "amount");
        grid.addColumn(app -> app.getBdzItem() != null ? app.getBdzItem().getName() : "").setHeader("Статья БДЗ");
        grid.addColumn(app -> app.getBoArticle() != null ? app.getBoArticle().getName() : "").setHeader("Статья БО");
        grid.addColumn(app -> app.getCfo() != null ? app.getCfo().getName() : "").setHeader("ЦФО");
        grid.addColumn(app -> app.getMvz() != null ? app.getMvz().getName() : "").setHeader("МВЗ");
        grid.asSingleSelect().addValueChangeListener(e -> edit(e.getValue()));
    }

    private FormLayout createForm() {
        ComboBox<BdzItem> bdzCombo = new ComboBox<>("Статья БДЗ");
        bdzCombo.setItems(bdzRepository.findAll());
        bdzCombo.setItemLabelGenerator(BdzItem::getName);

        boCombo = new ComboBox<>("Статья БО");
        boCombo.setItemLabelGenerator(BoArticle::getName);
        bdzCombo.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                boCombo.setItems(boRepository.findAll().stream().filter(b -> b.getBdzItem() == e.getValue()).toList());
            } else {
                boCombo.clear();
            }
        });

        ComboBox<Cfo> cfoCombo = new ComboBox<>("ЦФО");
        cfoCombo.setItems(cfoRepository.findAll());
        cfoCombo.setItemLabelGenerator(Cfo::getName);

        mvzCombo = new ComboBox<>("МВЗ");
        mvzCombo.setItemLabelGenerator(Mvz::getName);
        cfoCombo.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                mvzCombo.setItems(mvzRepository.findAll().stream().filter(m -> e.getValue().equals(m.getCfo())).toList());
            } else {
                mvzCombo.clear();
            }
        });

        TextField vgo = new TextField("ВГО");
        NumberField amount = new NumberField("Сумма");
        NumberField amountNoVat = new NumberField("Сумма без НДС");
        TextField subject = new TextField("Предмет договора");
        TextField period = new TextField("Период");
        TextField procurementMethod = new TextField("Способ закупки");

        binder.bind(bdzCombo, Application::getBdzItem, Application::setBdzItem);
        binder.bind(boCombo, Application::getBoArticle, Application::setBoArticle);
        binder.bind(cfoCombo, Application::getCfo, Application::setCfo);
        binder.bind(mvzCombo, Application::getMvz, Application::setMvz);
        binder.bind(vgo, Application::getVgo, Application::setVgo);
        binder.bind(amount, Application::getAmount, Application::setAmount);
        binder.bind(amountNoVat, Application::getAmountWithoutVat, Application::setAmountWithoutVat);
        binder.bind(subject, Application::getSubject, Application::setSubject);
        binder.bind(period, Application::getPeriod, Application::setPeriod);
        binder.bind(procurementMethod, Application::getProcurementMethod, Application::setProcurementMethod);

        Button save = new Button("Сохранить", e -> save());
        Button add = new Button("Добавить", e -> edit(new Application()));
        Button delete = new Button("Удалить", e -> delete());
        return new FormLayout(bdzCombo, boCombo, cfoCombo, mvzCombo, vgo, amount, amountNoVat, subject, period,
                procurementMethod, new HorizontalLayout(save, add, delete));
    }

    private void edit(Application application) {
        current = application;
        binder.setBean(application);
    }

    private void save() {
        Contract contract = current.getContract();
        if (contract != null && contract.getId() == null) {
            contractRepository.save(contract);
        }
        repository.save(current);
        updateList();
    }

    private void delete() {
        if (current != null && current.getId() != null) {
            repository.delete(current);
            updateList();
        }
    }

    private void updateList() {
        grid.setItems(repository.findAll());
    }
}
