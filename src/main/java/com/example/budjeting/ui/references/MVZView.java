package com.example.budjeting.ui.references;

import com.example.budjeting.model.CFO;
import com.example.budjeting.model.MVZ;
import com.example.budjeting.repository.CFORepository;
import com.example.budjeting.repository.MVZRepository;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

/**
 * CRUD view for MVZ entries.
 */
public class MVZView extends VerticalLayout {
    private final MVZRepository repo;
    private final CFORepository cfoRepo;
    private final Grid<MVZ> grid;

    public MVZView(MVZRepository repo, CFORepository cfoRepo) {
        this.repo = repo;
        this.cfoRepo = cfoRepo;
        setSizeFull();
        grid = new Grid<>(MVZ.class, false);
        grid.addColumn(MVZ::getCode).setHeader("Код");
        grid.addColumn(MVZ::getName).setHeader("Наименование");
        grid.addColumn(m -> {
            CFO c = m.getCfo();
            return c != null ? c.getName() : "";
        }).setHeader("ЦФО");
        refresh();

        Button add = new Button("Создать", e -> openForm(new MVZ()));
        Button edit = new Button("Редактировать", e -> {
            MVZ selected = grid.asSingleSelect().getValue();
            if (selected != null) {
                openForm(selected);
            }
        });
        Button delete = new Button("Удалить", e -> {
            MVZ selected = grid.asSingleSelect().getValue();
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

    private void openForm(MVZ item) {
        Dialog dialog = new Dialog();
        TextField code = new TextField("Код");
        TextField name = new TextField("Наименование");
        ComboBox<CFO> cfo = new ComboBox<>("ЦФО");
        cfo.setItems(cfoRepo.findAll());
        cfo.setItemLabelGenerator(CFO::getName);
        Button save = new Button("Сохранить", ev -> {
            item.setCode(code.getValue());
            item.setName(name.getValue());
            item.setCfo(cfo.getValue());
            repo.save(item);
            dialog.close();
            refresh();
        });
        VerticalLayout layout = new VerticalLayout(code, name, cfo, save);
        dialog.add(layout);
        if (item.getId() != null) {
            code.setValue(item.getCode());
            name.setValue(item.getName());
            cfo.setValue(item.getCfo());
        }
        dialog.open();
    }
}
