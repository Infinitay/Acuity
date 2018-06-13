package com.acuitybotting.website.acuity.navigation;

import com.acuitybotting.website.acuity.views.login.LoginView;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.spring.internal.Conventions;
import com.vaadin.spring.navigator.SpringNavigator;
import org.springframework.stereotype.Component;

/**
 * Governs view navigation of the app.
 */
@Component
@UIScope
public class SpringNavigationManager extends SpringNavigator {

	public void navigateTo(Class<? extends View> targetView) {
		String viewId = getViewId(targetView);
		navigateTo(viewId);
	}

	public void navigateTo(Class<? extends View> targetView, Object parameter) {
		String viewId = getViewId(targetView);
		navigateTo(viewId + "/" + parameter.toString());
	}

	@Override
	public void navigateTo(String navigationState) {
		if (null == this.getViewProvider(navigationState)) {
			navigationState = Conventions.upperCamelToLowerHyphen(navigationState);
		}
		super.navigateTo(navigationState);
	}

	public void navigateToDefaultView() {
		if (!getState().isEmpty()) {
			return;
		}
		navigateTo(LoginView.class);
	}

	public void updateViewParameter(String parameter) {
		String viewName = getViewId(getCurrentView().getClass());
		String parameters;
		if (parameter == null) {
			parameters = "";
		} else {
			parameters = parameter;
		}

		updateNavigationState(new ViewChangeEvent(this, getCurrentView(), getCurrentView(), viewName, parameters));
	}

	private static String getViewId(Class<? extends View> viewClass) {
		SpringView springView = viewClass.getAnnotation(SpringView.class);
		if (springView == null) {
			throw new IllegalArgumentException("The target class must be a @SpringView");
		}

		return Conventions.deriveMappingForView(viewClass, springView);
	}
}