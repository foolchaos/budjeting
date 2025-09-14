package com.example.budjeting.view;

import com.example.budjeting.entity.BdzItem;
import com.example.budjeting.entity.Supervisor;
import com.example.budjeting.repository.BdzItemRepository;
import com.example.budjeting.repository.SupervisorRepository;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.TreeGrid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "bdz", layout = MainLayout.class)
@PageTitle("Статьи БДЗ")
@PermitAll
public class BdzItemView extends VerticalLayout {

    private final BdzItemRepository repository;
    private final SupervisorRepository supervisorRepository;
    private final TreeGrid<BdzItem> grid = new TreeGrid<>();
    private final Binder<BdzItem> binder = new Binder<>(BdzItem.class);
    private BdzItem current;

    public BdzItemView(@Autowired BdzItemRepository repository,
                       @Autowired SupervisorRepository supervisorRepository) {
        this.repository = repository;
        this.supervisorRepository = supervisorRepository;
        configureGrid();
        add(grid, createForm());
        updateList();
    }

    private void configureGrid() {
        grid.addHierarchyColumn(BdzItem::getName).setHeader("Наименование");
        grid.addColumn(BdzItem::getCode).setHeader("Код");
        grid.addColumn(item -> item.getSupervisor() != null ? item.getSupervisor().getFullName() : "")
                .setHeader("Курирующий ЗГД");
        grid.asSingleSelect().addValueChangeListener(event -> editItem(event.getValue()));
    }

    private FormLayout createForm() {
        TextField name = new TextField("Наименование");
        TextField code = new TextField("Код");
        TextField supervisorName = new TextField("Курирующий ЗГД");

        binder.bind(name, BdzItem::getName, BdzItem::setName);
        binder.bind(code, BdzItem::getCode, BdzItem::setCode);
        binder.forField(supervisorName).bind(item -> item.getSupervisor() != null ? item.getSupervisor().getFullName() : "",
                (item, value) -> {
                    if (item.getSupervisor() == null) {
                        Supervisor sup = new Supervisor();
                        sup.setFullName(value);
                        sup.setBdzItem(item);
                        item.setSupervisor(sup);
                    } else {
                        item.getSupervisor().setFullName(value);
                    }
                });

        Button save = new Button("Сохранить", e -> save());
        Button add = new Button("Добавить", e -> editItem(new BdzItem()));
        Button delete = new Button("Удалить", e -> delete());
        HorizontalLayout buttons = new HorizontalLayout(save, add, delete);

        FormLayout form = new FormLayout(name, code, supervisorName, buttons);
        return form;
    }

    private void editItem(BdzItem item) {
        this.current = item;
        binder.setBean(item);
    }

    private void save() {
        repository.save(current);
        if (current.getSupervisor() != null) {
            supervisorRepository.save(current.getSupervisor());
        }
        updateList();
    }

    private void delete() {
        if (current != null && current.getId() != null) {
            repository.delete(current);
            updateList();
        }
    }

    private void updateList() {
        grid.setItems(repository.findAll(), BdzItem::getChildren);
    }
}
