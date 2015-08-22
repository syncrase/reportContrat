package rebean_wi.customers.Aerospatiale_Matra_Airbus.ID_187739;

import model.filters.Mode;
import model.filters.Severity;
import model.filters.Type;
import model.filters.testplan.Suite;
import model.filters.testplan.TestPlanType;
import model.filters.testplan.features.Features_REBEAN;

import org.junit.Test;

import tests.exported.annotations.BOTest;
import tests.exported.annotations.BOTestPlan;
import extensions.toolbox.WIStaticTestPlanDefinition;

@BOTestPlan(Type = TestPlanType.FEATURE_VERIFICATION, Suite = Suite.AURORA, Feat = Features_REBEAN.WI)	
public class CM_{cmNumber} extends WIStaticTestPlanDefinition{
	private static String _scriptID = "CM_{cmNumber}_{short
text}";
	public CM_{cmNumber} () {
		super(_scriptID);
	}
	
	@Test
	@BOTest(Objective = "Test CM_{cmNumber}_{short
text}' functionality", Severity = Severity.CRITICAL, Author = "", Mode = Mode.PROD, Type = Type.FUNCTIONAL)
	public void CM_{cmNumber}_{short text}" () {
		logNumericResults(launchTC(getTestcase("CM_{cmNumber}_{short text}")));
	}
}