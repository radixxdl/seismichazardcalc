package nzservs;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

import org.apache.commons.lang3.SystemUtils;

import org.opensha.commons.data.Site;
import org.opensha.commons.data.function.ArbitrarilyDiscretizedFunc;
import org.opensha.commons.geo.Location;
import org.opensha.sha.calc.*;
import org.opensha.sha.earthquake.*;
import org.opensha.sha.earthquake.rupForecastImpl.NewZealand.*;
import org.opensha.sha.gcim.calc.*;
import org.opensha.sha.gcim.imr.attenRelImpl.Bradley_2010_AttenRel;
import org.opensha.sha.imr.ScalarIMR;
import org.opensha.sha.imr.param.IntensityMeasureParams.*;
import org.opensha.sha.imr.param.SiteParams.*;
import org.opensha.sha.util.TectonicRegionType;

@WebServlet ("/nzhccServlet")
public class nzhccServlet extends HttpServlet {
	
	protected double lat;
	protected double lon;
	protected double vs30;
	protected double z1;
	protected double period;
	protected double disaggVal;
	
	protected Location location;
	protected Site site;
	protected Map<TectonicRegionType, ScalarIMR> imrMap;
	protected NewZealandERF2010 forecast;
	
	protected ArbitrarilyDiscretizedFunc func;
	protected HazardCurveCalculatorAPI calc;
	
	protected String disagg;
	protected nzhccResponse resp;
	
	protected static double duration = 50.0;
	
	protected final static double VS30_WARN_MIN = 150.0;
	protected final static double VS30_WARN_MAX = 1800.0;
	protected final static double DEPTH_1pt0_WARN_MIN = 0;
	protected final static double DEPTH_1pt0_WARN_MAX = 10000;
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws 
		ServletException, IOException {
		
        lat = Double.parseDouble(request.getParameter("lat"));
        lon = Double.parseDouble(request.getParameter("lon"));
        vs30 = Double.parseDouble(request.getParameter("vs30"));
        z1 = Double.parseDouble(request.getParameter("z1pt0"));
        period = Double.parseDouble(request.getParameter("period"));
        getHazardCurve();
        
        resp = new nzhccResponse();
        resp.hazFunction = func;
        
        if (request.getParameterMap().containsKey("disaggval")) {
        	disaggVal = Double.parseDouble(request.getParameter("disaggval"));
            getDisaggregatedCurve();
            resp.disaggWebAddr = disagg;
        }

        Gson gson = new Gson();
        String json = gson.toJson(resp);
        
        response.setContentType("application/json");        
        response.getWriter().write(json);        
	}
	
	public void getHazardCurve () {
	
		// Set site
		location = new Location(lat, lon);
		site = new Site(location);
		
		// Set site parameters		
		Vs30_Param vs30Param = new Vs30_Param(VS30_WARN_MIN, VS30_WARN_MAX);
		Vs30_TypeParam vs30_TypeParam = new Vs30_TypeParam();
		DepthTo1pt0kmPerSecParam depthTo1pt0kmPerSecParam 
			= new DepthTo1pt0kmPerSecParam(DEPTH_1pt0_WARN_MIN, DEPTH_1pt0_WARN_MAX);
		
		vs30Param.setValue(vs30);
		vs30_TypeParam.setValue(Vs30_TypeParam.VS30_TYPE_INFERRED);
		depthTo1pt0kmPerSecParam.setValue(z1);
		
		site.addParameter(vs30Param);
		site.addParameter(vs30_TypeParam);
		site.addParameter(depthTo1pt0kmPerSecParam);

		// Set IMR
		ScalarIMR imr = new Bradley_2010_AttenRel(null);
		imr.setParamDefaults();
		
		// Set SA to IMR
		imr.setIntensityMeasure(SA_Param.NAME);
		imr.getParameter(PeriodParam.NAME).setValue(period);		
		
		// Set site to IMR
		imr.setSite(site);
		
		// Create IMR Map
		TectonicRegionType trt = TectonicRegionType.ACTIVE_SHALLOW;
		imrMap = new HashMap<TectonicRegionType, ScalarIMR>();
		imrMap.put(trt, imr);
		
		// Set ERF
		forecast = new NewZealandERF2010();
		forecast.setParameter(forecast.FAULT_AND_BACK_SEIS_NAME, forecast.FAULT_AND_BACK_SEIS);	
		
		// Set default duration
		forecast.getTimeSpan().setDuration(duration);
		forecast.updateForecast();
		
		// Initialise hazard function
		func = new ArbitrarilyDiscretizedFunc();
		initHazXvalues(func);
		
		// Initialise hazard curve calculator
		calc = new HazardCurveCalculator();
		
		// Initialise magnitude distance cutoff function
		double[] cutoffMags = {0, 5, 6, 7, 8, 9};
		double[] cutoffDists = {-1e-16, 40, 80, 150, 250, 500};
		ArbitrarilyDiscretizedFunc magDistfunc = new ArbitrarilyDiscretizedFunc();
		
		for (int i = 0; i < cutoffMags.length; i++) {
			magDistfunc.set(cutoffDists[i], cutoffMags[i]);
		}
		
		calc.setMagDistCutoffFunc(magDistfunc);
		
		// Run hazard curve analysis
		func = (ArbitrarilyDiscretizedFunc) calc.getHazardCurve(func, site, imrMap, (ERF) forecast);
		func = toggleHazFuncLogValues(func);
		
	}
	
	public void getDisaggregatedCurve () {
		
		double minMag = 5.0;
		double deltaMag = 0.25;
		int numMag = 14; // 8.5 Mw
		double minDist = 0.1;
		double deltaDist = 10.0;
		int numDist = 15;
		int numSourcesForDisag = 100;
		double maxZAxis = 50;
		boolean showSourceDistances = false;
		
		// Initialise disaggregation calculator
		DisaggregationCalculatorAPI disaggCalc = new DisaggregationCalculator();
		disaggCalc.setDistanceRange(minDist, numDist, deltaDist);
		disaggCalc.setMagRange(minMag, numMag, deltaMag);
		disaggCalc.setNumSourcestoShow(numSourcesForDisag);
		disaggCalc.setShowDistances(showSourceDistances);
		
		double imlVal = func.getFirstInterpolatedX_inLogXLogYDomain(disaggVal);
		double probVal = disaggVal;
		
		long time = System.nanoTime();
		long t;
		
		disaggCalc.disaggregate(Math.log(imlVal), site, imrMap, (AbstractERF) forecast,
				calc.getMaxSourceDistance(), calc.getMagDistCutoffFunc());
//		t = System.nanoTime() - time;
//		System.out.println(t);
		
		disaggCalc.setMaxZAxisForPlot(maxZAxis);
//		t = System.nanoTime() - time;
//		System.out.println(t);
		
		String disaggregationPlotWebAddr = 
				disaggCalc.getDisaggregationPlotUsingServlet(getParametersInfoAsString());
//		t = System.nanoTime() - time;
//		System.out.println(t);
		
//		String disaggregationString = disaggCalc.getMeanAndModeInfo();
//		String sourceDisaggregationList = disaggCalc.getDisaggregationSourceInfo();
//		String binData = disaggCalc.getBinData();
//		String metadata = getMapParametersInfoAsHTML();		
//		String modeString = "Disaggregation Results for Prob = " + probVal
//		+ " (for IML = " + (float) imlVal + ")";
//		modeString += "\n" + disaggregationString;
		
		disagg = disaggregationPlotWebAddr;
			
	}
	
	public String getMapParametersInfoAsHTML () {
		
		String calcType = "Probabilistic";
		String imrMetadata = "IMR = Bradley (2010); "
				+ "Gaussian Truncation = None; "
				+ "Tectonic Region = Active Shallow Crust; "
				+ "Component = Average Horizontal; "
				+ "Std Dev Type = Total";
		String siteData = "Longitude = " + lon + "; "
				+ "Latitude = " + lat + "; "
				+ "Vs30 = " + vs30 + "; "
				+ "Vs30 Type = Inferred; "
				+ "Depth 1.0 km/sec = " + z1 + ";";
		String imtData = "IMT = SA; "
				+ "SA Period = " + period + "; "
				+ "SA Damping = 5.0";
		String erfData = "Eqk Rup Forecast = NewZealand_ERF_2010; "
				+ "Background and Fault Seismicity = Fault and Background Sources; "
				+ "Consider Epistemic Uncertainties = false";
		String timeData = "Duration = 50.0";
		String calcSettings = "Maximum Distance = 200.0; "
				+ "Num Event Sets = 1; "
				+ "Use Mag-Distance Filter? = false; "
				+ "null; "
				+ "Set TRT From Source? = false; "
				+ "If source TRT not supported by IMR = Use TRT value already set in IMR; "
				+ "Pt Src Dist Corr = None";
		
		return "<br>" + "Cacluation Type = " 
				+ calcType
				+ "<br><br>" 
				+ "IMR Param List:" + "<br>" 
				+ "---------------" + "<br>"
				+ imrMetadata
				+ "<br><br>"
				+ "Site Param List: " + "<br>"
				+ "----------------" + "<br>"
				+ siteData
				+ "<br><br>"
				+ "IMT Param List: " + "<br>"
				+ "---------------" + "<br>"
				+ imtData
				+ "<br><br>"
				+ "Forecast Param List: " + "<br>"
				+ "--------------------" + "<br>"
				+ erfData
				+ "<br><br>"
				+ "TimeSpan Param List: " + "<br>"
				+ "--------------------" + "<br>"
				+ timeData
				+ "<br><br>"
				+ "Calculation Settings: " + "<br>"
				+ "--------------------" + "<br>"
				+ calcSettings;

	}
	
	public String getParametersInfoAsString() {
		return getMapParametersInfoAsHTML().replaceAll("<br>",
				SystemUtils.LINE_SEPARATOR);
	}

	private static void initHazXvalues (ArbitrarilyDiscretizedFunc hazFunction) {		
		/** initialize the x values of the hazard curve */
		
		int num = 100;
		double xi = Math.log(0.0001);
		double xn = Math.log(10);
		double step = (xn - xi) / num;
		double xValue;
		
		for (int i = 0; i <= num; i++) {
			xValue = xi + i * step;
			hazFunction.set(xValue, 1.0);
		}		
	}

	private static ArbitrarilyDiscretizedFunc toggleHazFuncLogValues(ArbitrarilyDiscretizedFunc tempFunc) {
		/** Convert the x values of the hazard curve from log to the real numbers */
		
		int numPoints = tempFunc.size();
		double x, y;
		ArbitrarilyDiscretizedFunc hazFunction = new ArbitrarilyDiscretizedFunc();
		
		for(int i = 0; i < numPoints; i++){
			x = Math.exp(tempFunc.getX(i));
			y = tempFunc.getY(i);
			hazFunction.set(x,y);
		}
		return hazFunction;
	}
}