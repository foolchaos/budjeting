package com.example.budjeting.ui;

import com.example.budjeting.entity.Bdz;
import com.example.budjeting.repository.BdzRepository;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Справочники")
@Route(value = "", layout = MainLayout.class)
public class DirectoriesView extends HorizontalLayout {
    private final BdzRepository bdzRepository;
    private final Grid<Bdz> grid = new Grid<>(Bdz.class);
    private final Binder<Bdz> binder = new BeanValidationBinder<>(Bdz.class);
    private Bdz current;

    public DirectoriesView(BdzRepository bdzRepository) {
        this.bdzRepository = bdzRepository;
        setSizeFull();
        grid.setColumns("code", "name");
        grid.asSingleSelect().addValueChangeListener(e -> editBdz(e.getValue()));

        VerticalLayout form = new VerticalLayout();
        TextField code = new TextField("Код");
        TextField name = new TextField("Наименование");
        binder.bind(code, Bdz::getCode, Bdz::setCode);
        binder.bind(name, Bdz::getName, Bdz::setName);
        Button save = new Button("Сохранить", e -> save());
        Button add = new Button("Новая", e -> editBdz(new Bdz()));
        Button delete = new Button("Удалить", e -> delete());
        form.add(code, name, new HorizontalLayout(save, delete), add);

        add(grid, form);
        expand(grid);
        refresh();
    }

    private void refresh() {
        grid.setItems(bdzRepository.findAll());
    }

    private void editBdz(Bdz bdz) {
        if (bdz == null) {
            binder.setBean(null);
            current = null;
        } else {
            current = bdz;
            binder.setBean(bdz);
        }
    }

    private void save() {
        if (current != null) {
            bdzRepository.save(current);
            refresh();
        }
    }

    private void delete() {
        if (current != null && current.getId() != null) {
            bdzRepository.delete(current);
            editBdz(null);
            refresh();
        }
    }
}
