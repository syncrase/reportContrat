package aurora_customers.{ customerName};
public class CM_{CMNumber}_{text} extends MonoDocTestcase {
	MonoDocTestcaseConfigInfo _tccInfo;
	//private final static String _docPath = "";
	MSGStep _step = null;

	Public CM_{CMNumber}_{text} (MonoDocTestcaseConfigInfo tccInfo,String docName) {
		super(tccInfo, docName, "corporate", _docPath, true);
		_tccInfo = tccInfo;
	}
	@Override
	protected void run() throws Exception {
		startTestcaseStep("");
	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		// 
		_step = _msgHelper.startStep("");
		//Here, the implementation of the code
		_msgHelper.stopStep(_step);


		stopTestcaseStepWithoutActionWI();//Stop without compare
	}
}