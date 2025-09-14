package com.example.budjeting.view;

import com.example.budjeting.entity.Contract;
import com.example.budjeting.repository.ContractRepository;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "contracts", layout = MainLayout.class)
@PageTitle("Договоры")
public class ContractView extends VerticalLayout {

    private final ContractRepository repository;
    private final Grid<Contract> grid = new Grid<>(Contract.class);
    private final Binder<Contract> binder = new Binder<>(Contract.class);
    private Contract current;

    public ContractView(@Autowired ContractRepository repository) {
        this.repository = repository;
        configureGrid();
        add(grid, createForm());
        updateList();
    }

    private void configureGrid() {
        grid.setColumns("name", "internalNumber", "externalNumber", "contractDate", "responsible");
        grid.asSingleSelect().addValueChangeListener(e -> edit(e.getValue()));
    }

    private FormLayout createForm() {
        TextField name = new TextField("Наименование");
        TextField internalNumber = new TextField("Номер внутренний");
        TextField externalNumber = new TextField("Номер внешний");
        DatePicker contractDate = new DatePicker("Дата договора");
        TextField responsible = new TextField("Ответственный");

        binder.bind(name, Contract::getName, Contract::setName);
        binder.bind(internalNumber, Contract::getInternalNumber, Contract::setInternalNumber);
        binder.bind(externalNumber, Contract::getExternalNumber, Contract::setExternalNumber);
        binder.bind(contractDate, Contract::getContractDate, Contract::setContractDate);
        binder.bind(responsible, Contract::getResponsible, Contract::setResponsible);

        Button save = new Button("Сохранить", e -> save());
        Button add = new Button("Добавить", e -> edit(new Contract()));
        Button delete = new Button("Удалить", e -> delete());
        return new FormLayout(name, internalNumber, externalNumber, contractDate, responsible,
                new HorizontalLayout(save, add, delete));
    }

    private void edit(Contract contract) {
        current = contract;
        binder.setBean(contract);
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
