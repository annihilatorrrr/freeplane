package org.freeplane.main.application.survey;

import java.awt.Color;
import java.awt.Image;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.net.URL;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.freeplane.core.ui.components.UITools;
import org.freeplane.features.mode.Controller;

public class SurveyRunner {

	private static final String GO = "With pleasure";
	private static final String NOT_INTERESTED = "Not interested";
	private static final String REMIND_ME_LATER = "Remind me later";
	private static final String NEVER = "Don't ask me anything again";

	enum Options {GO_OPTION, NOT_INTERESTED_OPTION, REMIND_ME_LATER_OPTION, NEVER_OPTION};
	private static final int MINIMAL_DAYS_BETWEEN_SURVEYS = 11;
	private static final int MINIMAL_DAYS_BETWEEN_SURVEY_REMINDERS = 3;
	
	private final FreeplaneSurveyProperties freeplaneSurveyProperties;
	private String surveyId;
	private boolean userVisitedVotingLink = false;

	public SurveyRunner(FreeplaneSurveyProperties freeplaneSurveyProperties) {
		super();
		this.freeplaneSurveyProperties = freeplaneSurveyProperties;
	}

	public void runServey(String id, String title, String question, String surveyUrl) {
		if(! freeplaneSurveyProperties.mayAskUserToFillSurvey(surveyId))
			return;
		this.surveyId = id;
		freeplaneSurveyProperties.setNextSurveyDay(MINIMAL_DAYS_BETWEEN_SURVEYS);
		final String[] options = new String[]{GO, NOT_INTERESTED, REMIND_ME_LATER, NEVER};
		final List<Image> iconImages = UITools.getFrame().getIconImages();
		final JEditorPane messageComponent = new JEditorPane("text/html", question);
		messageComponent.setBorder( BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(Color.BLACK), BorderFactory.createEmptyBorder(10, 10, 10, 10)));
		messageComponent.addHyperlinkListener(new HyperlinkListener() {
			
			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					try {
						Controller.getCurrentController().getViewController().openDocument(e.getURL());
					} catch (Exception ex) {
					}
					userVisitedVotingLink = true;
					SwingUtilities.getWindowAncestor(messageComponent).setVisible(false);
				}
			}
		});
		messageComponent.setEditable(false);
		messageComponent.addHierarchyListener(new HierarchyListener() {
			
			@Override
			public void hierarchyChanged(HierarchyEvent e) {
				if(messageComponent.isShowing()){
					messageComponent.removeHierarchyListener(this);
					SwingUtilities.getWindowAncestor(messageComponent).setIconImages(iconImages);
				}
				
			}
		});
		final int userDecision = JOptionPane.showOptionDialog(UITools.getCurrentFrame(), messageComponent, title, JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION,
				null,
				options, REMIND_ME_LATER);
		switch(userDecision) {
		case JOptionPane.CLOSED_OPTION:
			if(userVisitedVotingLink)
				freeplaneSurveyProperties.markSurveyAsFilled(surveyId);
			break;
		default:
			switch(Options.values()[userDecision]){
			case GO_OPTION:
				try {
					freeplaneSurveyProperties.markSurveyAsFilled(surveyId);
					final URL survey = new URL(surveyUrl);
					Controller.getCurrentController().getViewController().openDocument(survey);
				} catch (Exception e) {
				}
				break;
			case NOT_INTERESTED_OPTION:
				freeplaneSurveyProperties.markSurveyAsFilled(surveyId);
				break;
			case REMIND_ME_LATER_OPTION:
				freeplaneSurveyProperties.setNextSurveyDay(MINIMAL_DAYS_BETWEEN_SURVEY_REMINDERS);
				break;
			case NEVER_OPTION:
				freeplaneSurveyProperties.setNeverShowSurvey();
				break;
			}
		}
	}
}
