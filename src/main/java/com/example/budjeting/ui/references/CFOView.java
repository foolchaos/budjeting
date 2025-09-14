package com.example.budjeting.ui.references;

import com.example.budjeting.model.CFO;
import com.example.budjeting.repository.CFORepository;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

/**
 * CRUD view for CFOs.
 */
public class CFOView extends VerticalLayout {
    private final CFORepository repo;
    private final Grid<CFO> grid;

    public CFOView(CFORepository repo) {
        this.repo = repo;
        setSizeFull();
        grid = new Grid<>(CFO.class, false);
        grid.addColumn(CFO::getCode).setHeader("Код");
        grid.addColumn(CFO::getName).setHeader("Наименование");
        refresh();

        Button add = new Button("Создать", e -> openForm(new CFO()));
        Button edit = new Button("Редактировать", e -> {
            CFO selected = grid.asSingleSelect().getValue();
            if (selected != null) {
                openForm(selected);
            }
        });
        Button delete = new Button("Удалить", e -> {
            CFO selected = grid.asSingleSelect().getValue();
            if (selected != null) {
                repo.delete(selected); // cascades MVZ
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

    private void openForm(CFO item) {
        Dialog dialog = new Dialog();
        TextField code = new TextField("Код");
        TextField name = new TextField("Наименование");
        Button save = new Button("Сохранить", ev -> {
            item.setCode(code.getValue());
            item.setName(name.getValue());
            repo.save(item);
            dialog.close();
            refresh();
        });
        VerticalLayout layout = new VerticalLayout(code, name, save);
        dialog.add(layout);
        if (item.getId() != null) {
            code.setValue(item.getCode());
            name.setValue(item.getName());
        }
        dialog.open();
    }
}
