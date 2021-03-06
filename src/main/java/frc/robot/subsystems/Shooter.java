package frc.robot.subsystems;

import edu.wpi.first.networktables.EntryListenerFlags;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;

import com.ctre.phoenix.motorcontrol.*;
import com.ctre.phoenix.motorcontrol.can.TalonFX;

import java.util.function.BooleanSupplier;


public class Shooter extends SubsystemBase {

// **********************************************
// Class Variables
// **********************************************

// **********************************************
// Instance Variables
// **********************************************


private double kP = 0.0;
private double kI = 0.0;
private double kF;
private double targetVelocity_UnitsPer100ms = Speed.FAR_HIGH_GOAL.getSpeed();
private double feederSpeed = Speed.FEEDER.getSpeed();

private boolean farSpeedTrue;

private enum Speed{
    FAR_HIGH_GOAL (20000),
    CLOSE_LOW_GOAL (7000),
    FEEDER (2000);

    private double speed;
    Speed (double speedIn){
        this.speed = speedIn;
    
    }
    public double getSpeed(){
        return this.speed;
    }
}


private TalonFX leftShooterMotor;
private TalonFX rightShooterMotor;
private int loops;


public NetworkTable shooterTable = NetworkTableInstance.getDefault().getTable(this.getClass().getSimpleName()); 
public NetworkTableEntry entryShooterPercentage = shooterTable.getEntry("Shooter percentage"); 
public NetworkTableEntry entryShooterSpeed = shooterTable.getEntry("Shooter speed"); 
public NetworkTableEntry entryLeftShooterVelocity = shooterTable.getEntry("Left Shooter Velocity"); 
public NetworkTableEntry entryRightShooterVelocity = shooterTable.getEntry("Right Shooter Velocity"); 
public NetworkTableEntry entryAverageShooterVelocity = shooterTable.getEntry("Average Shooter Velocity"); 
public NetworkTableEntry entryLeftMotorSupplyCurrent = shooterTable.getEntry("Left Motor Supply Current Draw"); 
public NetworkTableEntry entryLeftMotorStatorCurrent = shooterTable.getEntry("Left Motor Stator Current Draw"); 
public NetworkTableEntry entryLeftMotorVelocityError = shooterTable.getEntry("Left Motor Velocity Error"); 
public NetworkTableEntry entryLeftMotorTargetVelocity = shooterTable.getEntry("Left Motor Target Velocity (clicks/100ms)"); 
public NetworkTableEntry entryTargetVelocity = shooterTable.getEntry("Target Velocity"); 
public NetworkTableEntry entryF = shooterTable.getEntry("F coeficient"); 
public NetworkTableEntry entryP = shooterTable.getEntry("P coeficient"); 
public NetworkTableEntry entryI = shooterTable.getEntry("I coeficient"); 
public NetworkTableEntry entryD = shooterTable.getEntry("D coeficient"); 
public NetworkTableEntry entryFeederSpeed = shooterTable.getEntry("Feeder Speed");
 
// **********************************************
// Constructors
// **********************************************




public Shooter(){
    shooterTable.addEntryListener("Target Velocity", (table, key, entry, value, flags) ->{
        System.out.println("shooter target speed changed value: " + value.getValue());
        targetVelocity_UnitsPer100ms = entryTargetVelocity.getDouble(0);
    }, EntryListenerFlags.kNew | EntryListenerFlags.kUpdate);

    SmartDashboard.putNumber("Shooter kF: ", 0.0);

    kF = getkF();

    leftShooterMotor = new TalonFX(Constants.CanBusConfig.LAUNCHER);

    leftShooterMotor.setNeutralMode(NeutralMode.Coast);
    leftShooterMotor.setInverted(false);
    leftShooterMotor.configClosedloopRamp(0.5);
    leftShooterMotor.configSelectedFeedbackSensor(FeedbackDevice.IntegratedSensor, 0, 30);
    leftShooterMotor.setSensorPhase(true);
    leftShooterMotor.configNominalOutputForward(0, 30);
    leftShooterMotor.configPeakOutputForward(1, 30);
    leftShooterMotor.configNominalOutputReverse(0, 30);
    leftShooterMotor.configPeakOutputReverse(-1, 30);

    leftShooterMotor.config_kP(0, kP, 30);
    leftShooterMotor.config_kI(0, kI, 30);
    leftShooterMotor.config_kD(0, 0, 30);
    leftShooterMotor.config_kF(0, 0.05, 30);

    rightShooterMotor = new TalonFX(Constants.CanBusConfig.FEEDER);
    rightShooterMotor.setNeutralMode(NeutralMode.Brake);

    rightShooterMotor.setInverted(true);
    rightShooterMotor.configClosedloopRamp(0.5);
    rightShooterMotor.configSelectedFeedbackSensor(FeedbackDevice.IntegratedSensor, 0, 30);
    rightShooterMotor.setSensorPhase(true);
    rightShooterMotor.configNominalOutputForward(0, 30);
    rightShooterMotor.configPeakOutputForward(1, 30);
    rightShooterMotor.configNominalOutputReverse(0, 30);
    rightShooterMotor.configPeakOutputReverse(-1, 30);

    rightShooterMotor.config_kP(0, kP, 30);
    rightShooterMotor.config_kI(0, kI, 30);
    rightShooterMotor.config_kD(0, 0, 30);
    rightShooterMotor.config_kF(0, .08, 30);

    shooterTable.addEntryListener("P coeficient", (table, key, entry, value, flags) -> {
        kP = value.getDouble();
        System.out.println(String.format("p changed value: %.2f", kP));
        leftShooterMotor.config_kP(0, kP, 30);
        }, EntryListenerFlags.kNew | EntryListenerFlags.kUpdate);

    shooterTable.addEntryListener("I coeficient", (table, key, entry, value, flags) -> {
        kI = value.getDouble();
        System.out.println(String.format("I changed value: %.2f", kP));
        leftShooterMotor.config_kI(0, kI, 30);
        }, EntryListenerFlags.kNew | EntryListenerFlags.kUpdate);

    entryFeederSpeed.setDouble(feederSpeed);

}

public double getkF() {
    return SmartDashboard.getNumber("Shooter kF: ", 0.0);
}

public BooleanSupplier atVelocity() {
    return ()-> targetVelocity_UnitsPer100ms == leftShooterMotor.getSelectedSensorVelocity();
}




// **********************************************
// Instance Methods
// **********************************************

    public void activateShooter(){    
        leftShooterMotor.set(ControlMode.Velocity, targetVelocity_UnitsPer100ms);
    }

    

    public boolean farHighGoal(){
        targetVelocity_UnitsPer100ms = Speed.FAR_HIGH_GOAL.getSpeed();
        farSpeedTrue = true;
        return farSpeedTrue;
    }

    public boolean closeLowGoal(){
        targetVelocity_UnitsPer100ms = Speed.CLOSE_LOW_GOAL.getSpeed();
        farSpeedTrue = false;
        return farSpeedTrue;

    }
    
    public void reverseShooter(){
        leftShooterMotor.set(ControlMode.Velocity, -Speed.FEEDER.getSpeed());
    }

    public void startFeederMotor(){
        //rightShooterMotor.set(ControlMode.Velocity, feederSpeed);
        rightShooterMotor.set(ControlMode.PercentOutput, 1.0);
    }

    public void stopFeeder(){
        rightShooterMotor.set(ControlMode.PercentOutput, 0.0);
    }

    public void stopShooter(){
        leftShooterMotor.set(ControlMode.PercentOutput, 0.0); 
    }

    public double getSensorVelocity(){
        return leftShooterMotor.getSelectedSensorVelocity();    
    }

    public void periodic() {
        SmartDashboard.putBoolean("Far speed: ", farSpeedTrue);
        entryShooterPercentage.setDouble(leftShooterMotor.getMotorOutputPercent());
        entryLeftShooterVelocity.setDouble(leftShooterMotor.getSelectedSensorVelocity());
        entryLeftMotorSupplyCurrent.setDouble(leftShooterMotor.getSupplyCurrent());
        entryLeftMotorStatorCurrent.setDouble(leftShooterMotor.getStatorCurrent());
        if (leftShooterMotor.getControlMode() == ControlMode.Velocity){
            entryLeftMotorVelocityError.setDouble(leftShooterMotor.getClosedLoopError(0));
            entryLeftMotorTargetVelocity.setDouble(leftShooterMotor.getClosedLoopTarget(0));
        }
        entryTargetVelocity.setDouble(targetVelocity_UnitsPer100ms);
        entryP.setDouble(kP);
        entryI.setDouble(kI);
        
        entryRightShooterVelocity.setDouble(rightShooterMotor.getSelectedSensorVelocity());
        feederSpeed = entryFeederSpeed.getDouble(1000);

        StringBuilder sb = new StringBuilder();
        sb.append("\tout");
        sb.append("%");
        sb.append("\tspd");
        sb.append(leftShooterMotor.getSelectedSensorVelocity());
        sb.append("u");
        sb.append("\terr: ");
        sb.append(leftShooterMotor.getClosedLoopError(0));
        sb.append("\ttrg: ");
        sb.append(targetVelocity_UnitsPer100ms);
    

        if(++loops >= 10){
            loops = 0;
          //  System.out.println(sb.toString());
        }
        sb.setLength(0);
    }

    @Override
    public void register() {
        super.register();
    }

    @Override
    public void simulationPeriodic() {
        entryShooterPercentage.setDouble(leftShooterMotor.getMotorOutputPercent());
        entryLeftShooterVelocity.setDouble(leftShooterMotor.getSelectedSensorVelocity());
        entryLeftMotorSupplyCurrent.setDouble(leftShooterMotor.getSupplyCurrent());
        entryLeftMotorStatorCurrent.setDouble(leftShooterMotor.getStatorCurrent());
        if (leftShooterMotor.getControlMode() == ControlMode.Velocity){
            entryLeftMotorVelocityError.setDouble(leftShooterMotor.getClosedLoopError(0));
            entryLeftMotorTargetVelocity.setDouble(leftShooterMotor.getClosedLoopTarget(0));
            entryLeftMotorTargetVelocity.setDouble(leftShooterMotor.getClosedLoopTarget(0));
        }
        entryTargetVelocity.setDouble(targetVelocity_UnitsPer100ms);
        entryP.setDouble(kP);
        entryI.setDouble(kI);
        feederSpeed = entryFeederSpeed.getDouble(1000);
    }

} 
