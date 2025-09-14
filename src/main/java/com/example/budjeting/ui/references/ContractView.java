package com.example.budjeting.ui.references;

import com.example.budjeting.model.Contract;
import com.example.budjeting.repository.ContractRepository;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

/**
 * CRUD view for contracts.
 */
public class ContractView extends VerticalLayout {
    private final ContractRepository repo;
    private final Grid<Contract> grid;

    public ContractView(ContractRepository repo) {
        this.repo = repo;
        setSizeFull();
        grid = new Grid<>(Contract.class, false);
        grid.addColumn(Contract::getName).setHeader("Наименование");
        grid.addColumn(Contract::getInternalNumber).setHeader("Внутренний номер");
        grid.addColumn(Contract::getExternalNumber).setHeader("Внешний номер");
        grid.addColumn(c -> c.getContractDate() != null ? c.getContractDate().toString() : "").setHeader("Дата договора");
        grid.addColumn(Contract::getResponsible).setHeader("Ответственный");
        refresh();

        Button add = new Button("Создать", e -> openForm(new Contract()));
        Button edit = new Button("Редактировать", e -> {
            Contract selected = grid.asSingleSelect().getValue();
            if (selected != null) {
                openForm(selected);
            }
        });
        Button delete = new Button("Удалить", e -> {
            Contract selected = grid.asSingleSelect().getValue();
            if (selected != null) {
                repo.delete(selected);
                refresh();
            }
        });
        HorizontalLayout actions = new HorizontalLayout(add, edit, delete);
        add(actions, grid);
        setFlexGrow(1, grid);
    }

    private void refresh() {
        grid.setItems(repo.findAll());
    }

    private void openForm(Contract item) {
        Dialog dialog = new Dialog();
        TextField name = new TextField("Наименование");
        TextField internal = new TextField("Внутренний номер");
        TextField external = new TextField("Внешний номер");
        DatePicker date = new DatePicker("Дата договора");
        TextField responsible = new TextField("Ответственный");
        Button save = new Button("Сохранить", ev -> {
            item.setName(name.getValue());
            item.setInternalNumber(internal.getValue());
            item.setExternalNumber(external.getValue());
            item.setContractDate(date.getValue());
            item.setResponsible(responsible.getValue());
            repo.save(item);
            dialog.close();
            refresh();
        });
        VerticalLayout layout = new VerticalLayout(name, internal, external, date, responsible, save);
        dialog.add(layout);
        if (item.getId() != null) {
            name.setValue(item.getName());
            internal.setValue(item.getInternalNumber());
            external.setValue(item.getExternalNumber());
            date.setValue(item.getContractDate());
            responsible.setValue(item.getResponsible());
        }
        dialog.open();
    }
}
