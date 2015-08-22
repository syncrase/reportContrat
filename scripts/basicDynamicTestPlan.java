package rebean_wi.customers.{customerName}.{customerID};
//imports
@BOTestPlan(Type = TestPlanType.FEATURE_VERIFICATION, Suite = Suite.AURORA41, Feat = Features_REBEAN.WI)
public class CM_{CMNumber} extends WIDynamicTestPlanDefinition {
	private static String _scriptID = " CM_{CMNumber}_{text}";
	public CM_{CMNumber}() {
		super(_scriptID);
	}
	@Test
	@BOTest(Objective = "Test 'CM_801959_2014_ValuesMissing' functionality", Severity = Severity.CRITICAL, Author = "ptaquet", Mode = Mode.PROD, Type = Type.FUNCTIONAL)
	public void CM_{CMNumber}_{text}() {
	logNumericResults(launchTC(getTestcase("aurora_customers. .{customerName}.CM_{CMNumber}_{text}");
	}
}