package com.sap.jenkinsci.plugin.remote_view;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.Saveable;
import hudson.model.TopLevelItem;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.model.View;
import hudson.model.ViewDescriptor;
import hudson.util.DescribableList;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

/**
 * Created by @NutellaMitBrezel on 09.06.2015.
 */
public class RemoteJobsView extends View {
  private final static Logger logger = Logger.getLogger(RemoteJobsView.class.getName());

  private DescribableList<SectionedViewSection, Descriptor<SectionedViewSection>> sections;

  public Iterable<SectionedViewSection> getSections() {
    return sections;
  }

  @DataBoundConstructor
  public RemoteJobsView(String name) {
    super(name);
    initSections();
  }

  protected void initSections() {
    if (sections != null) { // already persisted
      return;
    }
    sections = new DescribableList<SectionedViewSection, Descriptor<SectionedViewSection>>(Saveable.NOOP);
  }

  /**
   * Handles the configuration submission.
   * 
   * Load view-specific properties here.
   */
  @Override
  protected void submit(StaplerRequest req) throws ServletException, FormException {
    if (sections == null) {
      sections = new DescribableList<SectionedViewSection, Descriptor<SectionedViewSection>>(Saveable.NOOP);
    }
    try {
      sections.rebuildHetero(req, req.getSubmittedForm(), Jenkins.getInstance()
          .<SectionedViewSection, Descriptor<SectionedViewSection>> getDescriptorList(SectionedViewSection.class),
          "sections");
    } catch (IOException e) {
      RemoteJobsView.logger.log(Level.SEVERE, "Error while rebulding sections list" + e.getMessage());
      throw new FormException("Error rebuilding list of sections.", e, "sections");
    }

    String[] val = req.getParameterValues("remoteURL");
    String[] val1 = req.getParameterValues("UserName");
    String[] val2 = req.getParameterValues("Password");

    int i = 0;
    for (SectionedViewSection s : sections) {

      if (val[i] != null && val1[i] != null && val2[i] != null) {
        s.setRemoteURL(val[i]);
        s.setUserName(val1[i]);
        s.setPassword(val2[i]);
        s.setCounter(i);
        i++;
      }

      // hasParameter() for Boolean
      s.setFilterEnabled(req.hasParameter(String.valueOf(s.getCounter()).concat("enabled")));
      s.setBlue(req.hasParameter(String.valueOf(s.getCounter()).concat("blue")));
      s.setYellow(req.hasParameter(String.valueOf(s.getCounter()).concat("yellow")));
      s.setRed(req.hasParameter(String.valueOf(s.getCounter()).concat("red")));
      s.setAborted(req.hasParameter(String.valueOf(s.getCounter()).concat("aborted")));

      // Save checkboxes
      List<RemoteJob> remoteJobs;
      remoteJobs = s.getAllJobs();
      s.setDisplayJobs(new HashMap<String, Wrapper>());
      s.setAvailable(true);

      // For each job save its status in variable displayJobs whether it should be displayed or not
      if (remoteJobs != null) {
        for (RemoteJob rj : remoteJobs) {
          try {
            String concat = String.valueOf(s.getCounter()) + (rj.getName() != null ? rj.getName() : "");
            boolean checked = req.hasParameter(concat);
            Wrapper wr = new Wrapper(rj.getName(), true);
            s.getDisplayJobs().put(concat, wr);
          } catch (Exception e) {
            RemoteJobsView.logger.log(Level.SEVERE, "Caught exception while processing remote job.", e);
          }
        }
      }
    }
  }

  @Extension
  public static final class DescriptorImpl extends ViewDescriptor {

    @Override
    public String getDisplayName() {
      return "Remote Jobs View";
    }
  }

  // We do not use the getItems method because we read the jobs from the API
  @Override
  public Collection<TopLevelItem> getItems() {
    List<TopLevelItem> items = new ArrayList<TopLevelItem>(1);
    return items;
  }

  @Override
  @RequirePOST
  public Item doCreateItem(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
    if (!Jenkins.get().hasPermission(Item.CREATE)) {
      throw new AccessDeniedException("You do not have permission to create items.");
    }
    return Jenkins.get().doCreateItem(req, rsp);
  }



  @Override
  public boolean contains(TopLevelItem item) {
    return false;
  }

}
