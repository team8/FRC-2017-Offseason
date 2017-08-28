package com.palyrobotics.frc2017.vision;

import edu.wpi.first.wpilibj.networktables.NetworkTable;
import org.json.simple.parser.ParseException;
import org.spectrum3847.RIOdroid.RIOdroid;
import com.palyrobotics.frc2017.config.Constants;
import com.palyrobotics.frc2017.util.logger.Logger;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Supplies wrapper methods for using adb to control the Android
 *
 * <h1><b>Fields</b></h1>
 * 	<ul>
 * 		<li>Instance and State variables:
 * 			<ul>
 * 				<li>{@link AndroidConnectionHelper#s_instance}: Private instance of this class (Singleton)</li>
 * 				<li>{@link AndroidConnectionHelper#m_connectionState}: Current state of connection (private)</li>
 * 				<li>{@link AndroidConnectionHelper#m_streamState}: Current state of streaming</li>
 * 				<li><b>See:</b>{@link ConnectionState}</li>
 * 			</ul>
 * 		</li>
 * 		<li>Utility variables:
 * 			<ul>
 * 				<li>{@link AndroidConnectionHelper#m_secondsAlive}: Private count of seconds the program has run for</li>
 * 				<li>{@link AndroidConnectionHelper#m_stateAliveTime}: Private count of seconds the state has run for</li>
 * 				<li>{@link AndroidConnectionHelper#m_adbServerCreated}: Private boolean representing existence an adb server</li>
 * 				<li>{@link AndroidConnectionHelper#m_visionRunning}: Private boolean representing whether vision program is currently running</li>
 * 				<li>{@link AndroidConnectionHelper#m_running}: Private boolean representing whether the thread is running</li>
 * 				<li>{@link AndroidConnectionHelper#mTesting}: Private boolean representing whether program is testing on a pc with
 * 																adb installed and included in the path	</li>
 * 			</ul>
 * 		</li>
 * 	</ul>
 *
 * <h1><b>Accessors and Mutators</b></h1>
 * 	<ul>
 * 		<li>{@link AndroidConnectionHelper#getInstance()}</li>
 * 		<li>{@link AndroidConnectionHelper#SetState(ConnectionState)}</li>
 * 		<li>{@link AndroidConnectionHelper#SetStreamState(StreamState)}</li>
 * 	</ul>
 *
 * <h1><b>External Access Functions</b>
 * 	<br><BLOCKQUOTE>For using as a wrapper for RIOdroid</BLOCKQUOTE></h1>
 * 	<ul>
 * 		<li>{@link AndroidConnectionHelper#start()}</li>
 * 		<li>{@link AndroidConnectionHelper#StartVisionApp()}</li>
 * 	</ul>
 *
 * 	<h1><b>Internal Functions</b>
 * 	 <br><BLOCKQUOTE>Paired with external access functions. These compute the actual function for the external access</BLOCKQUOTE></h1>
 * 	 <ul>
 * 	     <li>{@link AndroidConnectionHelper#InitializeServer()}</li>
 * 	     <li>{@link AndroidConnectionHelper#VisionInit()}</li>
 * 	     <li>{@link AndroidConnectionHelper#StreamVision()}</li>
 * 	 </ul>
 *
 * @see ConnectionState
 * @see StreamState
 * @author Alvin
 *
 */
public class AndroidConnectionHelper implements Runnable{

	/**
	 * State of connection between the roboRIO and nexus
	 *
	 * <ul>
	 *     <li>{@link ConnectionState#PREINIT}</li>
	 *     <li>{@link ConnectionState#STARTING_SERVER}</li>
	 *     <li>{@link ConnectionState#IDLE}</li>
	 *     <li>{@link ConnectionState#START_VISION_APP}</li>
	 * </ul>
	 */
	public enum ConnectionState{
		PREINIT, STARTING_SERVER, IDLE, START_VISION_APP, STREAMING;
	}

	/**
	 * State of streaming data from Nexus
	 *
	 * <ul>
	 *     <li>{@link StreamState#IDLE}</li>
	 *     <li>{@link StreamState#JSON}</li>
	 * </ul>
	 */
	public enum StreamState{
		IDLE, JSON
	}

	// Instance and state variables
	private static AndroidConnectionHelper s_instance;
	private ConnectionState m_connectionState = ConnectionState.PREINIT;
	private StreamState m_streamState = StreamState.IDLE;

	// Utility variables
	private double m_secondsAlive = 0;
	private double m_stateAliveTime = 0;
	private boolean m_adbServerCreated = false;
	private boolean m_visionRunning = false;
	private boolean m_running = false;
	private boolean mTesting = false;

	private double m_x_dist = 0, m_z_dist = 0;
	private String m_androidState = "NONE";
	private Object m_android_lock = new Object();


	/**
	 * Creates an AndroidConnectionHelper instance
	 * Cannot be called outside as a Singleton
	 */
	private AndroidConnectionHelper(){}

	/**
	 * @return The instance of the ACH
	 */
	public static AndroidConnectionHelper getInstance(){
		if(s_instance == null){
			s_instance = new AndroidConnectionHelper();
		}
		return s_instance;
	}

	/**
	 * Sets the state of connection
	 * @param state State to switch to
	 */
	private void SetState(ConnectionState state){
		m_connectionState = state;
	}

	/**
	 * Sets the state of streaming between the Nexus
	 * @param state State to switch to
	 */
	private void SetStreamState(StreamState state){
		if(m_streamState.equals(state)){
			System.out.println("Warning: in AndroidConnectionHelper.SetStreamState(), "
					+ "no chane to write state");
		}else{
			m_streamState = state;
		}
	}

	/**
	 * Starts the AndroidConnectionHelper thread
	 */
	public void start(){
		this.start(false);
	}

	/**
	 * Starts the AndroidConnectionHelper thread
	 * <br>(accounts for running program for testing)
	 * @param isTesting
	 */
	public void start(boolean isTesting){

		if(m_connectionState != ConnectionState.PREINIT) {    // This should never happen
			System.out.println("Error: in AndroidConnectionHelper.start(), "
					+ "connection is already initialized");
		}

		if(m_running){	// This should never happen
			System.out.println("Error: in AndroidConnectionHelper.start(), "
					+ "thread is already running");
		}

		// Initialize Thread Variables
		this.SetState(ConnectionState.STARTING_SERVER);
		m_running = true;
		m_streamState = StreamState.JSON;
		this.mTesting = isTesting;

		System.out.println("Starting Thread: AndroidConnectionHelper ");
		(new Thread(this, "AndroidConnectionHelper")).start();

	}

	/**
	 * Initializes RIOdroid and RIOadb
	 * @return The state after execution
	 */
	private ConnectionState InitializeServer() {

		if(!this.isNexusConnected()){
//			System.out.println("Error: in AndroidConnectionHelper.InitializeServer(), " +
//					"nexus is not connected");
			return this.m_connectionState;
		}

		if(m_adbServerCreated){
			if(!this.isAppStarted()){
				System.out.println("Vision app not started, starting app");
				Logger.getInstance().logRobotThread("Vision app not started, starting app");

				this.VisionInit();
			} else {
				this.m_visionRunning = true;
			}

			if (this.m_visionRunning) {
				System.out.println("Connected to vision app");
				Logger.getInstance().logRobotThread("Connected to vision app");
				return ConnectionState.STREAMING;
			} else {
				System.out.println("Could not start vision app, retrying");
				Logger.getInstance().logRobotThread("Could not start vision app, retrying");
				return this.m_connectionState;
			}
		} else {
			this.InitializeAdbServer();
			return this.m_connectionState;
		}

	}

	private void InitializeAdbServer() {
		boolean connected = false;

		try {
			// Initializes RIOdroid usb and RIOadb adb daemon
			if (!this.mTesting) {
				RIOdroid.init();

				// Forward the port and start the server socket for vision
				RIOdroid.executeCommand("adb reverse tcp:" +
						Constants.kAndroidVisionSocketPort + " tcp:" +
						Constants.kAndroidVisionSocketPort);
				System.out.println("Starting VisionServerThread");
				VisionServerThread.getInstance().start(mTesting);
			} else {
				RuntimeExecutor.getInstance().init();

				// Forward the port and start the server socket for vision
				RuntimeExecutor.getInstance().exec("adb reverse tcp:" +
						Constants.kAndroidVisionSocketPort + " tcp:" +
						Constants.kAndroidVisionSocketPort);
				System.out.println("Starting VisionServerThread");
				VisionServerThread.getInstance().start(mTesting);
			}

			connected = true;
		} catch (Exception e) {
			System.out.println("Error: in AndroidConnectionHelper.InitializeServer(), "
					+ "could not connect.\n" + e.getStackTrace());
		}

		if(connected){      // Adb server started successfully
			m_adbServerCreated = true;
			System.out.println("Adb server started");
			Logger.getInstance().logRobotThread("Adb server started");
		} else {            // Adb server failed to start
			System.out.println("Failed to start adb server");
			Logger.getInstance().logRobotThread("Failed to start adb server");
		}

//		// Let it retry connection for 10 seconds, then give in
//		if (m_secondsAlive - m_stateAliveTime > 10 && !connected) {
//			System.out.println("Error: in AndroidConnectionHelper.InitializeServer(), "
//					+ "connection timed out");
//		}

	}

	/**
	 * Starts up the vision app
	 */
	public void StartVisionApp(){
//		if(!m_adbServerCreated){    // No abd server, can't start app
//			System.out.println("Warning: on call AndroidConnectionHelper.StartVisionApp(), " +
//					"adb server not started, abandoning app startup");
////			return;
//		}

		if(m_visionRunning){	// This should never happen, but easily can due to outside calling
			System.out.println("Warning: On call AndroidConnectionHelper.StartVisionApp(), "
					+ "vision app already running (or function has been called before)");
		}else{
			if(m_connectionState.equals(ConnectionState.STARTING_SERVER)){
				int limit = 40;
				int count = 0;
				while(!m_connectionState.equals(ConnectionState.IDLE)){
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if(count >= limit){
						break;
					}
					count++;
				}

			}else if(!m_connectionState.equals(ConnectionState.IDLE)){
				System.out.println("Error: in AndroidConnectionHelper.StartVisionApp(), "
						+ "connection not in a state to start app");
			}

			this.SetState(ConnectionState.START_VISION_APP);
		}
	}

	/**
	 * Sends command to boot up the vision app
	 * @return The state after execution
	 */
	private ConnectionState VisionInit(){
		boolean connected = false;
		try {
			// Starts app through adb shell, and stores the returned console message (for debugging)
			String outp = null;
			if(!this.mTesting) {
				outp = RIOdroid.executeCommand(
						"adb shell am start -n " + Constants.kPackageName + "/" +
								Constants.kPackageName + "." + Constants.kActivityName);
			}else{
				outp = RuntimeExecutor.getInstance().exec(
						"adb shell am start -n " + Constants.kPackageName + "/" +
								Constants.kPackageName + "." + Constants.kActivityName);
			}

			connected = true;
		}catch (Exception e) {
			System.out.println("Error: in AndroidConnectionHelper.VisionInit(), "
					+ "could not connect.\n" + e.getStackTrace());
		}

		if(connected) {     // App started successfully
			m_visionRunning = true;
			System.out.println("Starting Vision Stream");
			return ConnectionState.STREAMING;
		} else {            // Failed to start app
			return m_connectionState;
		}
	}

	/**
	 * Streams in the vision data
	 * @return The state after execution
	 */
	private ConnectionState StreamVision(){
		if(!m_visionRunning){	// This should never happen
			System.out.println("Error: in AndroidConnectionHelper.StreamVision(), "
					+ "vision program i not running (or has not been initialized inside this program)");
		}

		switch (m_streamState){
			case IDLE:
				System.out.println("Error: in AndroidConnectionHelper.StreamVision(), "
						+ "streaming in IDLE state, nothing streaming");
				break;
			case JSON:
				this.extractData(this.StreamJSON());
				break;
		}

		return m_connectionState;
	}

	/**
	 * Streams vision data via pulling a JSON file with
	 * data written to it
	 */
	private JSONObject StreamJSON(){
		String raw_data;

		// Read the JSON file which stores the vision data
		if(!this.mTesting){
			raw_data = RIOdroid.executeCommand("adb shell run-as "+Constants.kPackageName+" cat /data/data/"+ Constants.kPackageName
					+ "/files/data.json");
		}else{
			raw_data = RuntimeExecutor.getInstance().exec("adb shell run-as "+Constants.kPackageName+" cat /data/data/"+ Constants.kPackageName
					+ "/files/data.json");
		}

		return parseJSON(raw_data);
	}

	/**
	 * Computes parsing of streamed data (for now just prints to console)
	 * @param raw_data Raw JSON formatted data (String)
	 */
	private JSONObject parseJSON(String raw_data){
		if(raw_data == null  || raw_data.equals("") || raw_data.equals("error: no devices/emulators found")){
			return null;
		}

		// Create JSONObject from the raw String data
		JSONObject json = null;

		try {
			JSONParser parser = new JSONParser();
			json = (JSONObject) parser.parse(raw_data);
		} catch (ParseException e) {
			// This is spammy
//			e.printStackTrace();
		}

		return json;
	}

	private void extractData(JSONObject json){
		// Compute based on app state (given in the data)
		if(json != null){
			String state = (String) json.get("state");
			if(!(state == null) && !state.equals("")){	// Handle based on state
				synchronized (m_android_lock) {
					if (state.equals("STREAMING")) {
						// Get image data
						Number
							data_x = (Number)json.get("x_displacement"),
							data_z = (Number)json.get("z_displacement");
						if (data_x != null)
							this.m_x_dist = data_x.doubleValue();
						if (data_z != null)
							this.m_z_dist = data_z.doubleValue();
						//System.out.println("<x,z>: <"+this.m_x_dist+", "+this.m_z_dist+">");
					}
					m_androidState = state;
				}
			}
		}
	}

	public void setFlash(boolean isFlashOn){
		String out;
		if(!mTesting){
			out = RIOdroid.executeCommand("adb shell am broadcast -a "+Constants.kPackageName+".GET_DATA --es type flash --ez isFlash "+isFlashOn);
		}else{
			out = RuntimeExecutor.getInstance().exec("adb shell am broadcast -a "+Constants.kPackageName+".GET_DATA --es type flash --ez isFlash "+isFlashOn);
		}
	}

	public double getXDist(){
		if(!m_visionRunning) {
//			System.out.println("Error in AndroidConnectionHelper.getXDist(), " +
//					"no connection to vision app, returning default/last valid value");
		} else if(!m_androidState.equals("STREAMING")){
//			System.out.println("Warning in AndroidConnectionHelper.getXDist(), " +
//					"not streaming, android state is "+m_androidState+", returning last valid x_distance");
		}
		return m_x_dist;
	}
	
	public double getZDist() {
		return m_z_dist;
	}
	
	public boolean isNexusConnected(){
		boolean hasDevice = false;
		String[] outp = RuntimeExecutor.getInstance().exec("adb devices").split("\\n");

		for(int i=1; i<outp.length && !hasDevice; i++){
			hasDevice = outp[i].contains("device");
		}
		return hasDevice;
	}

	public boolean isAppStarted(){
		JSONObject json = this.StreamJSON();

		if (json != null) {
			String state = (String) json.get("state");
			if (state != null && !state.equals("")) {
				if(state.equals("STREAMING")){
					return true;
				}
			}
		}

		return false;
	}

	public boolean isServerStarted(){
		return m_adbServerCreated;
	}

	/**
	 * Updates the thread at {@link Constants#kAndroidConnectionUpdateRate} ms
	 */
	@Override
	public void run() {
		while(m_running){
			ConnectionState initState = m_connectionState;
			switch(m_connectionState){

				case PREINIT:	// Shouldn't happen, but can due to error
					System.out.println("Error: in AndroidConnectionHelper.run(), "
							+ "thread running on preinit state");
					break;

				case STARTING_SERVER:	// Triggered by start(), should be called externally
					this.SetState(this.InitializeServer());
					break;

				case START_VISION_APP:	// Triggered by StartVisionApp(), should be called externally
					this.SetState(this.VisionInit());
					break;

				case STREAMING:
					this.SetState(this.StreamVision());
					break;

				case IDLE:
					break;
			}

			// Reset state start time if state changed
			if (!initState.equals(m_connectionState)) {
				m_stateAliveTime = m_secondsAlive;
			}

			// Handle thread sleeping, sleep for set constant update delay
			try {
				Thread.sleep(Constants.kAndroidConnectionUpdateRate);
				m_secondsAlive += Constants.kAndroidConnectionUpdateRate/1000.0;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
