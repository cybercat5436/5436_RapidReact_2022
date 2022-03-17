package frc.robot.subsystems;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.ctre.phoenix.motorcontrol.can.TalonFX;
import com.ctre.phoenix.motorcontrol.can.VictorSPX;
import com.revrobotics.CANSparkMax;
import com.revrobotics.RelativeEncoder;
import com.revrobotics.SparkMaxPIDController;
import com.revrobotics.CANSparkMax.ControlType;
import com.revrobotics.CANSparkMax.IdleMode;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;

import edu.wpi.first.networktables.EntryListenerFlags;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants.CanBusConfig;
import frc.robot.Constants.ClimberConfig;

public class Climber extends SubsystemBase {
// **********************************************
// Class Variables
// **********************************************


// **********************************************
// Instance Variables
// **********************************************
// Actuators
private TalonFX innerArmMotor;
private TalonFX outerArmMotor;
private VictorSPX solenoidMotor;
private CANSparkMax tiltMotor;

// Sensors
private DigitalInput tiltRetractLimit;
private RelativeEncoder tiltEncoder;

// Controllers
private SparkMaxPIDController tiltPIDController;

// arm speed variables
private double innerArmSpeed = 0.6;
private double outerArmSpeed = 0.8;
private double tiltSpeed = 0.3;

private final double startSpeed = 0.5;

private boolean solenoidEngaged = false;

private boolean ignoreEncoder = true;
private boolean resetInnerArmEncoder = false;
private boolean resetOuterArmEncoder = false;
private boolean resetTiltEncoder = false;

private double tiltTimeLimit = 1.5;



private final String netTblName = "Climber";
private NetworkTable netTblClimber = NetworkTableInstance.getDefault().getTable(netTblName);

private NetworkTableEntry entryCurrentCommand= netTblClimber.getEntry("Climber Command");

// Arm Speed Set Points
private NetworkTableEntry entryInnerArmSpeed= netTblClimber.getEntry("Inner Arm Speed");
private NetworkTableEntry entryTiltSpeed= netTblClimber.getEntry("Tilt Speed");
private NetworkTableEntry entryOuterArmSpeed= netTblClimber.getEntry("Outer Arm Speed");

// Actual Motor Speeds
private NetworkTableEntry entryInnerArmMotorSpeed= netTblClimber.getEntry("InnerArmMotorSpeed");
private NetworkTableEntry entryOuterArmMotorSpeed= netTblClimber.getEntry("OuterArmMotorSpeed");
private NetworkTableEntry entryTiltMotorSpeed= netTblClimber.getEntry("TiltMotorSpeed");

// Encoder Resets
private NetworkTableEntry entryResetInnerArmEncoder = netTblClimber.getEntry("Reset Inner Arm Encoder");
private NetworkTableEntry entryResetOuterArmEncoder = netTblClimber.getEntry("Reset Outer Arm Encoder");
private NetworkTableEntry entryResetTiltEncoder = netTblClimber.getEntry("Reset Tilt Encoder");

// Encoder override
private NetworkTableEntry entryIgnoreEncoder = netTblClimber.getEntry("Ignore Encoder");

// Encoder Setpoints
private NetworkTableEntry entryTiltTimeLimit = netTblClimber.getEntry("Tilt Time Limit");
private NetworkTableEntry entryTiltRetractLimit = netTblClimber.getEntry("Tilt Retract Limit Switch");

// Solenoid State
private NetworkTableEntry entrySolenoidEngaged = netTblClimber.getEntry("SolenoidEngaged");

// Encoder Values
private NetworkTableEntry entryOuterArmPos = netTblClimber.getEntry("Outer Encoder");
private NetworkTableEntry entryInnerArmPos = netTblClimber.getEntry("Inner Encoder");
private NetworkTableEntry entryTiltMotorPos = netTblClimber.getEntry("Tilt Encoder");

// Tilt PIDF Values
private NetworkTableEntry entryTiltKp = netTblClimber.getEntry("Tilt kP");
private NetworkTableEntry entryTiltKi = netTblClimber.getEntry("Tilt kI");
private NetworkTableEntry entryTiltKd = netTblClimber.getEntry("Tilt kD");
private NetworkTableEntry entryTiltKf = netTblClimber.getEntry("Tilt kF");


// **********************************************
// Constructors
// **********************************************

public Climber(){
    System.out.println(String.format("Entering %s::%s", this.getClass().getSimpleName(), new Throwable().getStackTrace()[0].getMethodName()));
    

    init();
    setNetworkTableListeners();




}

// **********************************************
// Getters & Setters
// **********************************************

    public boolean getIgnoreEncoder(){
        return this.ignoreEncoder;
    }

    public boolean getIsTiltFullyRetracted(){
        return !tiltRetractLimit.get();
    }

    public double getTiltTimeLimit(){
        return tiltTimeLimit;
    }

    public boolean getAreOuterArmsTooLong(){
        // returns true when the limit switch is engaged and the outer arms exceed the encoder position
        return getOuterClimberPosition() >= ClimberConfig.OUTER_LEGAL_LIMIT && getIsTiltFullyRetracted();
      }

    public double getClimberPosition(){
        return innerArmMotor.getSelectedSensorPosition();
    }

    public double getOuterClimberPosition(){
        return outerArmMotor.getSelectedSensorPosition();
    }

// **********************************************
// Class Methods
// **********************************************


// **********************************************
// Instance Methods
// **********************************************

    public NetworkTableEntry getEntryTiltTimeLimit() {
        return entryTiltTimeLimit;
    }

    public void setEntryTiltTimeLimit(NetworkTableEntry entryTiltTimeLimit) {
        this.entryTiltTimeLimit = entryTiltTimeLimit;
    }

    public void resetSpeed(){
        innerArmSpeed = startSpeed;
    }

    public void innerArmUp(){
        // todo:make speed variable
        innerArmMotor.set(ControlMode.PercentOutput, -innerArmSpeed);
        // rightMotor.set(climbSpeed);
    }

    public void innerArmDown(){
        innerArmMotor.set(ControlMode.PercentOutput, innerArmSpeed);
        // rightMotor.set(-climbSpeed);
    }

    public void stopAndEngageRatchet(){
        innerArmMotor.set(ControlMode.PercentOutput, 0.0);
        engageRatchet();
        // rightMotor.set(0);
    }

    public void stop(){
        innerArmMotor.set(ControlMode.PercentOutput, 0.0);
    }
    /*
    public void increaseSpeed(){
        double newSpeed = climbSpeed += rateOfChange;
        if(newSpeed > 1.0) {
            newSpeed = 1;
        } else if(newSpeed < 0){
            newSpeed = rateOfChange;
        }
        climbSpeed = newSpeed;
    } 

    public void decreaseSpeed(){
        double newSpeed = climbSpeed -= rateOfChange;
        if(newSpeed <= 0.0){
            newSpeed = rateOfChange;
        } else if (newSpeed >1){
            newSpeed = 1;
        }
        climbSpeed = newSpeed;
    } */

    public void startTilt(String direction){
        double speed = 0.0;

        if(direction.equals("forward")) {
            speed = tiltSpeed;            
        } else if(direction.equals("retract")) {
            speed = -tiltSpeed;
        }

        tiltMotor.set(speed);
    }

    public void stopTilt(){
        tiltMotor.set(0.0);
    }

    public void tiltToPosition(double targetPosition){
        tiltPIDController.setReference(targetPosition, ControlType.kPosition);
    }

    public void startOuterArms(String direction){
        // limits speed between 0.1 - 0.5
        //outerArmSpeed = xboxController.getRawAxis(outerArmAxis.value) / 2.0;
        double speed = 0.0;

        if(direction.equalsIgnoreCase("forward")){
            speed = outerArmSpeed;
        } else if(direction.equalsIgnoreCase("retract")) {
           speed = -outerArmSpeed;
        }
        outerArmMotor.set(ControlMode.PercentOutput, speed);
    }

    public void stopOuterArms(){
//outerArmSpeed = 0.0;
        outerArmMotor.set(ControlMode.PercentOutput, 0.0);
    }

    public void engageRatchet(){
        solenoidMotor.set(ControlMode.PercentOutput, 0);
        solenoidEngaged = true;
    }

    public void releaseRatchet(){
        solenoidMotor.set(ControlMode.PercentOutput, 100);
        solenoidEngaged = false;
    }

    public void resetEncoder(){
        innerArmMotor.setSelectedSensorPosition(0.0);
    }


    @Override
    public void periodic() {
        // System.out.println(String.format("Entering %s::%s", this.getClass().getSimpleName(), new Throwable().getStackTrace()[0].getMethodName()));
        
        // update current command
        entryCurrentCommand.setString((this.getCurrentCommand() == null) ? "None" : this.getCurrentCommand().getName());
        
        // Update actual motor speeds
        entryInnerArmMotorSpeed.setDouble(innerArmMotor.getMotorOutputPercent());
        entryOuterArmMotorSpeed.setDouble(outerArmMotor.getMotorOutputPercent());
        entryTiltMotorSpeed.setDouble(tiltMotor.get());
        
        // Update tiltRetractLimit switch state
        entryTiltRetractLimit.setBoolean(tiltRetractLimit.get());

        // update state of the solenoid
        entrySolenoidEngaged.setBoolean(solenoidEngaged);
        
        // update encoder  positions
        entryOuterArmPos.setDouble(outerArmMotor.getSelectedSensorPosition());
        entryInnerArmPos.setDouble(innerArmMotor.getSelectedSensorPosition());
        entryTiltMotorPos.setDouble(tiltEncoder.getPosition());

    }

    public void init() {
        // instantiate motors
        innerArmMotor = new TalonFX(CanBusConfig.INNER_ARM);
        outerArmMotor = new TalonFX(CanBusConfig.OUTER_ARM);
        solenoidMotor = new VictorSPX(CanBusConfig.SOLENOID);
        tiltMotor = new CANSparkMax(CanBusConfig.TILT, MotorType.kBrushless);
        
        //instantiate sensors
        tiltRetractLimit = new DigitalInput(9);
        tiltEncoder = tiltMotor.getEncoder();
    
        // clear all faults
        innerArmMotor.clearStickyFaults();
        outerArmMotor.clearStickyFaults();
        solenoidMotor.clearStickyFaults();
        tiltMotor.clearFaults();
    
        // set factory default for all motors
        innerArmMotor.configFactoryDefault();
        outerArmMotor.configFactoryDefault();
        solenoidMotor.configFactoryDefault();
        tiltMotor.restoreFactoryDefaults();
        
        // set braking mode for all
        innerArmMotor.setNeutralMode(NeutralMode.Brake);
        outerArmMotor.setNeutralMode(NeutralMode.Brake);
        solenoidMotor.setNeutralMode(NeutralMode.Brake);
        tiltMotor.setIdleMode(IdleMode.kBrake);
    
        // Invert motors
        innerArmMotor.setInverted(true);
        tiltMotor.setInverted(true);
    
        // Setup PID controller for tilt Motor
        tiltPIDController = tiltMotor.getPIDController();
        tiltPIDController.setP(0.2);
        
        // engage the ratchet
        engageRatchet();

        // zero the endoers
        innerArmMotor.setSelectedSensorPosition(0.0);
        outerArmMotor.setSelectedSensorPosition(0.0);
        tiltEncoder.setPosition(0.0);

    }

    //bind to button if needed
    /*
    public void stopAtLimit() {
        
        if(!climberLimit.get()) {
            stop();
        } else {
            //do nothing
        }
    } */

    @Override
    public void register() {
        super.register();
    }

    @Override
    public void simulationPeriodic() {
        super.simulationPeriodic();
    }
    
    public void setNetworkTableListeners(){
        
        // Create listeners for arm speeds and set initial values
        // *****************************************************************************************
        // Inner Arm
        entryInnerArmSpeed.setDouble(innerArmSpeed);
        String innerArmEntryName = NetworkTable.basenameKey(entryInnerArmSpeed.getName());
        netTblClimber.addEntryListener(innerArmEntryName, (table, key, entry, value, flags)->{
            System.out.println("The value for innerArmSpeed changed");
            if (innerArmSpeed != value.getDouble()){
                System.out.println("Updating inner arm speed from network table");
                innerArmSpeed = value.getDouble();
            }
        },  EntryListenerFlags.kNew | EntryListenerFlags.kUpdate);

        // Outer Arm
        entryOuterArmSpeed.setDouble(outerArmSpeed);
        String outerArmEntryName = NetworkTable.basenameKey(entryOuterArmSpeed.getName());
        netTblClimber.addEntryListener(outerArmEntryName, (table, key, entry, value, flags)-> {
            System.out.println("The value for outerArmSpeed changed");
            if (outerArmSpeed != value.getDouble()){
                System.out.println("Updating the instance att based on table data");
                outerArmSpeed = value.getDouble();
            }
        },  EntryListenerFlags.kNew | EntryListenerFlags.kUpdate);

        // Tilt Motoer
        entryTiltSpeed.setDouble(tiltSpeed);
        String tiltEntryName = NetworkTable.basenameKey(entryTiltSpeed.getName());
        netTblClimber.addEntryListener(tiltEntryName, (table, key, entry, value, flags)-> {
            System.out.println("The value for tiltSpeed changed");
            if (tiltSpeed != value.getDouble()){
                System.out.println("Updating the instance att based on table data");
                tiltSpeed = value.getDouble();
            }
        },  EntryListenerFlags.kNew | EntryListenerFlags.kUpdate);
        // *****************************************************************************************



        // Encoder Resets
        // *****************************************************************************************
        // Inner Arm
        entryResetInnerArmEncoder.setBoolean(resetInnerArmEncoder);
        String resetInnerEncoderEntryName = NetworkTable.basenameKey(entryResetInnerArmEncoder.getName());
        netTblClimber.addEntryListener(resetInnerEncoderEntryName, (table, key, entry, value, flags)->{
            System.out.println("Inner Arm Encoder being reset");
            if (value.getBoolean()){
                System.out.println("Resetting inner arm encoder");
                innerArmMotor.setSelectedSensorPosition(0);
                resetInnerArmEncoder = false;
                entryResetInnerArmEncoder.setBoolean(resetInnerArmEncoder);
            }
        },  EntryListenerFlags.kNew | EntryListenerFlags.kUpdate);
        
        // Outer Arm
        entryResetOuterArmEncoder.setBoolean(resetOuterArmEncoder);
        String resetOuterArmEncoderEntryName = NetworkTable.basenameKey(entryResetOuterArmEncoder.getName());
        netTblClimber.addEntryListener(resetOuterArmEncoderEntryName, (table, key, entry, value, flags)->{
            if (value.getBoolean()){
                System.out.println("Resetting Outer Arm Encoder");
                outerArmMotor.setSelectedSensorPosition(0);
                resetOuterArmEncoder = false;
                entryResetOuterArmEncoder.setBoolean(resetOuterArmEncoder);
            }
        },  EntryListenerFlags.kNew | EntryListenerFlags.kUpdate);
        
        // Tilt Motor
        entryResetTiltEncoder.setBoolean(resetTiltEncoder);
        String resetTiltEncoderEntryName = NetworkTable.basenameKey(entryResetTiltEncoder.getName());
        netTblClimber.addEntryListener(resetTiltEncoderEntryName, (table, key, entry, value, flags)->{
            if (value.getBoolean()){
                System.out.println("Resetting Tilt Encoder");
                tiltEncoder.setPosition(0.0);
                resetTiltEncoder = false;
                entryResetTiltEncoder.setBoolean(resetTiltEncoder);
            }
        },  EntryListenerFlags.kNew | EntryListenerFlags.kUpdate);
        // *****************************************************************************************
        
        

        // Encoder override
        // *****************************************************************************************
        entryIgnoreEncoder.setBoolean(ignoreEncoder);
        String ignoreEncoderEntryName = NetworkTable.basenameKey(entryIgnoreEncoder.getName());
        netTblClimber.addEntryListener(ignoreEncoderEntryName, (table, key, entry, value, flags)->{
            if (value.getBoolean()!=ignoreEncoder){
                System.out.println("Changing Ignore Encoders");    
                ignoreEncoder = value.getBoolean();
            }
        },  EntryListenerFlags.kNew | EntryListenerFlags.kUpdate);
        // *****************************************************************************************


        // Tilt Time Limit
        // *****************************************************************************************
        entryTiltTimeLimit.setDouble(tiltTimeLimit);
        String tiltTimeLimitEntryName = NetworkTable.basenameKey(entryTiltTimeLimit.getName());
        netTblClimber.addEntryListener(tiltTimeLimitEntryName, (table, key, entry, value, flags)->{
            double proposedValue = value.getDouble();
            boolean isPositive = proposedValue > 0;
            boolean isWithinUpperBound = proposedValue < 5.0;
            boolean isNewValue = proposedValue != tiltTimeLimit;
            if (isNewValue && isPositive && isWithinUpperBound){
                System.out.println("Changing Tilt Time Limit");    
                tiltTimeLimit = proposedValue;
            } else {
                // set the entry back
                entryTiltTimeLimit.setDouble(tiltTimeLimit);
            }
        },  EntryListenerFlags.kNew | EntryListenerFlags.kUpdate);
        // *****************************************************************************************

        // Tilt PID Limits
        // *****************************************************************************************
        // kP
        entryTiltKp.setDouble(tiltPIDController.getP());
        String tiltKpEntryName = NetworkTable.basenameKey(entryTiltKp.getName());
        netTblClimber.addEntryListener(tiltKpEntryName, (table, key, entry, value, flags)->{
            double proposedValue = value.getDouble();
            boolean isPositive = proposedValue >= 0;
            boolean isWithinUpperBound = proposedValue < 1.0;
            boolean isNewValue = proposedValue != tiltPIDController.getP();
            if (isNewValue && isPositive && isWithinUpperBound){
                System.out.println("Changing Tilt kP");    
                tiltPIDController.setP(proposedValue);
            } else {
                // set the entry back
                entryTiltKp.setDouble(tiltPIDController.getP());
            }
        },  EntryListenerFlags.kNew | EntryListenerFlags.kUpdate);

        // kI
        entryTiltKi.setDouble(tiltPIDController.getI());
        String tiltKiEntryName = NetworkTable.basenameKey(entryTiltKi.getName());
        netTblClimber.addEntryListener(tiltKiEntryName, (table, key, entry, value, flags)->{
            double proposedValue = value.getDouble();
            boolean isPositive = proposedValue >= 0;
            boolean isWithinUpperBound = proposedValue < 1.0;
            boolean isNewValue = proposedValue != tiltPIDController.getI();
            if (isNewValue && isPositive && isWithinUpperBound){
                System.out.println("Changing Tilt kI");    
                tiltPIDController.setI(proposedValue);
            } else {
                // set the entry back
                entryTiltKi.setDouble(tiltPIDController.getI());
            }
        },  EntryListenerFlags.kNew | EntryListenerFlags.kUpdate);

        // kD
        entryTiltKd.setDouble(tiltPIDController.getD());
        String tiltKdEntryName = NetworkTable.basenameKey(entryTiltKd.getName());
        netTblClimber.addEntryListener(tiltKdEntryName, (table, key, entry, value, flags)->{
            double proposedValue = value.getDouble();
            boolean isPositive = proposedValue >= 0;
            boolean isWithinUpperBound = proposedValue < 1.0;
            boolean isNewValue = proposedValue != tiltPIDController.getD();
            if (isNewValue && isPositive && isWithinUpperBound){
                System.out.println("Changing Tilt kD");    
                tiltPIDController.setD(proposedValue);
            } else {
                // set the entry back
                entryTiltKd.setDouble(tiltPIDController.getD());
            }
        },  EntryListenerFlags.kNew | EntryListenerFlags.kUpdate);

        // kF
        entryTiltKf.setDouble(tiltPIDController.getFF());
        String tiltKfEntryName = NetworkTable.basenameKey(entryTiltKf.getName());
        netTblClimber.addEntryListener(tiltKfEntryName, (table, key, entry, value, flags)->{
            double proposedValue = value.getDouble();
            boolean isPositive = proposedValue >= 0;
            boolean isWithinUpperBound = proposedValue < 1.0;
            boolean isNewValue = proposedValue != tiltPIDController.getFF();
            if (isNewValue && isPositive && isWithinUpperBound){
                System.out.println("Changing Tilt kF");    
                tiltPIDController.setFF(proposedValue);
            } else {
                // set the entry back
                entryTiltKd.setDouble(tiltPIDController.getFF());
            }
        },  EntryListenerFlags.kNew | EntryListenerFlags.kUpdate);
        // *****************************************************************************************

    }
    
}
