package com.example.budjeting.ui.references;

import com.example.budjeting.model.BudgetArticle;
import com.example.budjeting.model.Supervisor;
import com.example.budjeting.repository.BudgetArticleRepository;
import com.example.budjeting.repository.SupervisorRepository;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

/**
 * CRUD view for supervisors.
 */
public class SupervisorView extends VerticalLayout {
    private final SupervisorRepository repo;
    private final BudgetArticleRepository budgetRepo;
    private final Grid<Supervisor> grid;

    public SupervisorView(SupervisorRepository repo, BudgetArticleRepository budgetRepo) {
        this.repo = repo;
        this.budgetRepo = budgetRepo;
        setSizeFull();
        grid = new Grid<>(Supervisor.class, false);
        grid.addColumn(Supervisor::getFullName).setHeader("ФИО");
        grid.addColumn(Supervisor::getDepartment).setHeader("Департамент");
        grid.addColumn(s -> {
            BudgetArticle ba = s.getBudgetArticle();
            return ba != null ? ba.getName() : "";
        }).setHeader("Статья БДЗ");
        refresh();

        Button add = new Button("Создать", e -> openForm(new Supervisor()));
        Button edit = new Button("Редактировать", e -> {
            Supervisor selected = grid.asSingleSelect().getValue();
            if (selected != null) {
                openForm(selected);
            }
        });
        Button delete = new Button("Удалить", e -> {
            Supervisor selected = grid.asSingleSelect().getValue();
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

    private void openForm(Supervisor item) {
        Dialog dialog = new Dialog();
        TextField name = new TextField("ФИО");
        TextField dept = new TextField("Департамент");
        ComboBox<BudgetArticle> budget = new ComboBox<>("Статья БДЗ");
        budget.setItems(budgetRepo.findAll());
        budget.setItemLabelGenerator(BudgetArticle::getName);
        Button save = new Button("Сохранить", ev -> {
            item.setFullName(name.getValue());
            item.setDepartment(dept.getValue());
            item.setBudgetArticle(budget.getValue());
            repo.save(item);
            dialog.close();
            refresh();
        });
        VerticalLayout layout = new VerticalLayout(name, dept, budget, save);
        dialog.add(layout);
        if (item.getId() != null) {
            name.setValue(item.getFullName());
            dept.setValue(item.getDepartment());
            budget.setValue(item.getBudgetArticle());
        }
        dialog.open();
    }
}
