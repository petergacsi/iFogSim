package org.fog.test.perfeval;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.placement.Controller;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacementEdgewards;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;

public class DISSECT_To_IFogSim {
	
	
	static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
	static List<Sensor> sensors = new ArrayList<Sensor>();
	static List<Actuator> actuators = new ArrayList<Actuator>();
	
	//
	static List<FogDevice> fogDevice_Type1 = new ArrayList<>();
	static List<FogDevice> fogDevice_Type2 = new ArrayList<>();
	static List<FogDevice> clouds = new ArrayList<>();
	
	
	static double EEG_TRANSMISSION_TIME = 5.1;
	static int NUMBER_OF_CLOUDS = 1;
	static int NUMBER_OF_TYPE_1_FOGDEVICE = 4;
	static int NUMBER_OF_TYPE_2_FOGDEVICE_PER_TYPE_1_FOG = 3;
	static int NUMBER_OF_STATIONS_PER_FOG = 3;
	
	
	public static void main(String[] args) {
		
		Log.printLine("Start simulation...");
		
		try {
		
			Log.disable();
			int num_user = 1; 
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false;
	
			CloudSim.init(num_user, calendar, trace_flag);
			
			String appId = "test";
			FogBroker broker = new FogBroker("broker");
			
			Application application = createApplication(appId, broker.getId());
			application.setUserId(broker.getId());
			createCloud(broker.getId(), appId);
			
			ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();
			moduleMapping.addModuleToDevice("cloud", "cloud");
			
			Controller controller = new Controller("master-controller", fogDevices, sensors, actuators);
			controller.submitApplication(application, 0, new ModulePlacementEdgewards(fogDevices, sensors, actuators, application, moduleMapping));
			
			
			TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());
	

			for (FogDevice d : fogDevices) {
				System.out.println(d.getName());
			}
			
			
			
			CloudSim.startSimulation();
	
			CloudSim.stopSimulation();
			Log.printLine("Simulation finished");
			
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("Error during simulation");
		}
		
	}
	
	
	
	private static void createCloud(int userId, String appId) {
		FogDevice cloud = createDefaultDevice("cloud", 45000, 40000, 10000, 10000, 0, 0.01, 16*103, 16*83.25);
		cloud.setParentId(-1);
		fogDevices.add(cloud);
		clouds.add(cloud);
		
		for (int i=0; i< NUMBER_OF_TYPE_1_FOGDEVICE; i++) {
			FogDevice Type1_fogDevice = createDefaultDevice("fogDevice_Type1-"+i, 3000, 4000, 10000, 10000, 1, 0.0, 107.339, 83.4333); 
			fogDevices.add(Type1_fogDevice);
			fogDevice_Type1.add(Type1_fogDevice);
			Type1_fogDevice.setParentId(cloud.getId());
			Type1_fogDevice.setUplinkLatency(4);
		
			for (int y = 0; y < NUMBER_OF_TYPE_2_FOGDEVICE_PER_TYPE_1_FOG ; y++) {
				FogDevice Type2_fogDevice = createDefaultDevice("fogDevice_Type2-"+ i + "-"+ y, 1000, 1000, 10000, 270, 2, 0, 87.53, 82.44);
				fogDevices.add(Type2_fogDevice);
				fogDevice_Type2.add(Type2_fogDevice);
				Type2_fogDevice.setParentId(Type1_fogDevice.getId());
				Type2_fogDevice.setUplinkLatency(2);
				
				for (int x = 0; x < NUMBER_OF_STATIONS_PER_FOG; x++) {
					createStation(""+ i+ "-" + y + "-" +x, userId, appId, Type2_fogDevice.getId());
				}
			}
		}
	}
	


	private static void createStation(String id, int userId, String appId, int parentID) {
		Sensor sensor = new Sensor("s-"+id, "STATION_DATA", userId, appId, new DeterministicDistribution(EEG_TRANSMISSION_TIME));
		sensors.add(sensor);
		sensor.setGatewayDeviceId(parentID);
		sensor.setLatency(6.0);
	
	
	}
	
	private static FogDevice createDefaultDevice(String nodeName, long mips, int ram,
			long upBw, long downBw, int level, double ratePerMips, double busyPower, double idlePower){
		
		
		List<Pe> peList = new ArrayList<Pe>();
		peList.add(new Pe(0, new PeProvisionerOverbooking(mips)));
		
		int hostId = FogUtils.generateEntityId();
		long storage = 1000000;
		int bw = 10000;
		
		PowerHost host = new PowerHost(
				hostId,
				new RamProvisionerSimple(ram),
				new BwProvisionerOverbooking(bw),
				storage,
				peList,
				new StreamOperatorScheduler(peList),
				new FogLinearPowerModel(busyPower, idlePower)
				);
		
		List<Host> hostList = new ArrayList<Host>();
		hostList.add(host);
		
		
		String arch = "x86"; // system architecture
		String os = "Linux"; // operating system
		String vmm = "Xen";
		double time_zone = 10.0; // time zone this resource located
		double cost = 3.0; // the cost of using processing in this resource
		double costPerMem = 0.05; // the cost of using memory in this resource
		double costPerStorage = 0.001; // the cost of using storage in this
										// resource
		double costPerBw = 0.0; // the cost of using bw in this resource
		LinkedList<Storage> storageList = new LinkedList<Storage>(); // we are not adding SAN
													// devices by now
		
		FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
				arch, os, vmm, host, time_zone, cost, costPerMem,
				costPerStorage, costPerBw);

		FogDevice fogdevice = null;
		try {
			fogdevice = new FogDevice(nodeName, characteristics, 
					new AppModuleAllocationPolicy(hostList), storageList, 10, upBw, downBw, 0, ratePerMips);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		fogdevice.setLevel(level);
		return fogdevice;
	}
	
	@SuppressWarnings({"serial" })
	private static Application createApplication(String appId, int userId) {
		
		Application application = Application.createApplication(appId, userId);
		application.addAppModule("fog", 10);
		application.addAppModule("cloud", 10);
		
		application.addAppEdge("STATION_DATA", "fog", 2500, 500, "STATION_DATA", Tuple.UP, AppEdge.SENSOR);
		application.addAppEdge("fog", "cloud", 2500, 500, "PROCESSED_DATA", Tuple.UP, AppEdge.MODULE);
		application.addTupleMapping("fog", "STATION_DATA", "PROCESSED_DATA", new FractionalSelectivity(1));
		
		ArrayList<String>list1 = new ArrayList<String>(){{add("STATION_DATA");add("fog");add("cloud");}}; 
		final AppLoop loop1 = new AppLoop(list1);
		
		List<AppLoop> loops = new ArrayList<AppLoop>() {{add(loop1);}};
		application.setLoops(loops);
		
		return application;
	}
		
} 


