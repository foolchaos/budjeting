package com.example.budjeting.view;

import com.example.budjeting.entity.Supervisor;
import com.example.budjeting.repository.SupervisorRepository;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "supervisors", layout = MainLayout.class)
@PageTitle("Курирующие ЗГД")
public class SupervisorView extends VerticalLayout {

    private final SupervisorRepository repository;
    private final Grid<Supervisor> grid = new Grid<>(Supervisor.class);
    private final Binder<Supervisor> binder = new Binder<>(Supervisor.class);
    private Supervisor current;

    public SupervisorView(@Autowired SupervisorRepository repository) {
        this.repository = repository;
        configureGrid();
        add(grid, createForm());
        updateList();
    }

    private void configureGrid() {
        grid.setColumns("fullName", "department");
        grid.asSingleSelect().addValueChangeListener(e -> edit(e.getValue()));
    }

    private FormLayout createForm() {
        TextField fullName = new TextField("ФИО");
        TextField department = new TextField("Департамент");
        binder.bind(fullName, Supervisor::getFullName, Supervisor::setFullName);
        binder.bind(department, Supervisor::getDepartment, Supervisor::setDepartment);
        Button save = new Button("Сохранить", e -> save());
        Button add = new Button("Добавить", e -> edit(new Supervisor()));
        Button delete = new Button("Удалить", e -> delete());
        return new FormLayout(fullName, department, new HorizontalLayout(save, add, delete));
    }

    private void edit(Supervisor supervisor) {
        current = supervisor;
        binder.setBean(supervisor);
    }

    private void save() {
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
