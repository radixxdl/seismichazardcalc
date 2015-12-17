package nzservs;

import org.opensha.commons.data.function.ArbitrarilyDiscretizedFunc;

import com.google.gson.Gson;

public class testClass {
	
	public static void main(String[] args) {
		
		nzhccServlet serv = new nzhccServlet();
		nzhccResponse resp = new nzhccResponse();
		
		serv.lon = 174.8;
		serv.lat = -41.3;
		serv.vs30 = 250;
		serv.z1 = 320;
		serv.period = 1.0;
		
		serv.disaggVal = 0.5;
		
		serv.getHazardCurve();		
        resp.hazFunction = serv.func;
        
		//serv.getDisaggregatedCurve();
        //resp.disaggWebAddr = serv.disagg;

        Gson gson = new Gson();
        String json = gson.toJson(resp);
        
        System.out.println(json);
		
	}

}