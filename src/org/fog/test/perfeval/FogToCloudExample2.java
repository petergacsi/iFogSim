package org.fog.test.perfeval;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

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

public class FogToCloudExample2 {
	
	
	static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
	static List<Sensor> sensors = new ArrayList<Sensor>();
	static List<Actuator> actuators = new ArrayList<Actuator>();
	
	static double EEG_TRANSMISSION_TIME = 10;
	static int FogDevicesNumber = 4;
	static int StationNumber = 15;
	
	public static void main(String[] args) {
		
		Log.printLine("Start simulation...");
		
		try {
		//create an application
		
			Log.disable();
			int num_user = 1; // number of cloud users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false; // mean trace events
	
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
			Log.printLine("something bad happened");
		}
		
	}
	
	
	
	private static void createCloud(int userId, String appId) {
		FogDevice cloud = createDefaultDevice("cloud", 44800, 40000, 100, 10000, 0, 0.01, 16*103, 16*83.25);
		cloud.setParentId(-1);
		fogDevices.add(cloud);
		
		for (int i=0; i< FogDevicesNumber; i++) {
			FogDevice fogDevice = createDefaultDevice("fogDevice-"+i, 280, 1000, 500, 500, 1, 0.0, 107.339, 83.4333); 
			fogDevices.add(fogDevice);
			fogDevice.setParentId(cloud.getId());
			fogDevice.setUplinkLatency(100);
		}
		
		for (int i = 0; i< StationNumber; i++) {

			createStation(""+i, userId, appId);
			
		}
		
		
		
	}
	
	private static FogDevice getRandomFogDevice() {
		Random rng = new Random();
		List<Integer> onlyFogs = new ArrayList<Integer>();
		for (FogDevice fogdevice : fogDevices) {
			if (fogdevice.getName().startsWith("f")) {
				onlyFogs.add(fogdevice.getId());
			}
		}
		int random = rng.nextInt(onlyFogs.size())+1;
		FogDevice randomFogDevice = fogDevices.get(random);
		
		return randomFogDevice;
		
	}
	
	
	private static void createStation(String id, int userId, String appId) {
		
		Sensor sensor = new Sensor("s-"+id, "STATION_DATA", userId, appId, new DeterministicDistribution(EEG_TRANSMISSION_TIME));
		sensors.add(sensor);
		Actuator display = new Actuator("actuator-"+id, userId, appId, "DISPLAY");
		actuators.add(display);
		FogDevice gateWayFog = getRandomFogDevice();
		sensor.setGatewayDeviceId(gateWayFog.getId());
		sensor.setLatency(6.0);
		display.setGatewayDeviceId(gateWayFog.getId());
		display.setLatency(1.0);
		
		
	
	}
	
	private static FogDevice createDefaultDevice(String nodeName, long mips, int ram,
			long upBw, long downBw, int level, double ratePerMips, double busyPower, double idlePower){
		
		
		List<Pe> peList = new ArrayList<Pe>();
		peList.add(new Pe(0, new PeProvisionerOverbooking(mips)));
		
		int hostId = FogUtils.generateEntityId();
		long storage = 1000000; // host storage
		int bw = 10000;
		
		//létrehozzuk az erõforrás kezelõ osztályokat, melyek a metrikákban is segítenek
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
		
		//Modul ram fog max.: 4000, cloud max: 40000
		//Try different values 
		application.addAppModule("fog", 1);
		application.addAppModule("cloud", 12);
		
		
		//MIPS 
		application.addAppEdge("STATION_DATA", "fog", 200, 1000, "STATION_DATA", Tuple.UP, AppEdge.SENSOR);
		application.addAppEdge("fog", "cloud", 200, 1000, "PROCESSED_DATA", Tuple.UP, AppEdge.MODULE);
			
		application.addTupleMapping("fog", "STATION_DATA", "PROCESSED_DATA", new FractionalSelectivity(1));
		
		
		ArrayList<String>list1 = new ArrayList<String>(){{add("STATION_DATA");add("fog");add("cloud");}}; 
		final AppLoop loop1 = new AppLoop(list1);
		
		
		List<AppLoop> loops = new ArrayList<AppLoop>() {{add(loop1);}};
		application.setLoops(loops);
		
		
		return application;
	}
		
} 


