/**
 * Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)
 * @author git-hemant@github.com
 */
package github.hemant.lc;

import github.hemant.lc.config.Config;
import github.hemant.lc.config.ConfigLoadException;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

/**
 * Main executable class which schedules when we should be running the jobs and
 * run the jobs as scheduled.
 */
public class InvestmentScheduler {

	static {
		System.setProperty("org.quartz.scheduler.skipUpdateCheck", "true");
	}
	
	public static void main(String[] args) 
			throws ConfigLoadException, ParseException, SchedulerException, IOException {
		Config config = Config.loadConfig();
		
		if (config.cronSchedule() == null) {
			System.out.println("Executing it one time as schedule is not specified.");
			AutomaticInvestor.invest();
		} else {
			List<String> cronSchedule = config.cronSchedule();
			if (cronSchedule.size() == 0) {
				System.err.println("Please specify valid running schedule or remove \"schedule\" attribute from the config file.");
			} else {
				System.err.println("Configured investment schedule to run at " + cronSchedule.size() + " scheduled time.");
				schedule(cronSchedule);
			}
		}
	}
	
	public static void schedule(List<String> cronSchedule) throws ParseException, SchedulerException {
		SchedulerFactory factory = new StdSchedulerFactory();
		Scheduler scheduler = factory.getScheduler();
		Map<JobDetail, Set<? extends Trigger>> triggersAndJobs = new HashMap<JobDetail, Set<? extends Trigger>>();
		Set<CronTrigger> triggerList = new HashSet<CronTrigger>();
		String groupName = "GroupName";
		String jobName = "JobName";
		JobKey jobKey = new JobKey(jobName, groupName);
		String triggerName = jobName;
		JobDetail job = JobBuilder.newJob(InvestmentJob.class).withIdentity(jobKey).usingJobData("groupName", groupName)
				.usingJobData("jobName", jobName).build();
				
		for (int i = 0, size = cronSchedule.size(); i < size; i++) {
			CronTrigger trigger = (CronTrigger) TriggerBuilder.newTrigger().withIdentity(triggerName + i, groupName)
					.withSchedule(CronScheduleBuilder.cronSchedule(cronSchedule.get(i))).build();
			triggerList.add(trigger);
		}
		triggersAndJobs.put(job, triggerList);
		scheduler.start();
		scheduler.scheduleJobs(triggersAndJobs, true);
	}
	
	// This class must be public.
	public static class InvestmentJob implements Job {
		public void execute(JobExecutionContext context) throws JobExecutionException {
			AutomaticInvestor.invest();
		}
	}
	
}
