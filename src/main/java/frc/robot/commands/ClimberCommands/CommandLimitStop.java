package frc.robot.commands.ClimberCommands;

import edu.wpi.first.wpilibj2.command.CommandBase;
import frc.robot.subsystems.Climber;

public class CommandLimitStop extends CommandBase {
    private Climber climber;
    
    // **********************************************
    // Constructors
    // **********************************************

    public CommandLimitStop(Climber climber) {
        System.out.println(String.format("Entering %s::%s", this.getClass().getSimpleName(), new Throwable().getStackTrace()[0].getMethodName()));
        this.climber = climber;
        this.addRequirements(climber);
    }

    // **********************************************
    // Getters & Setters
    // **********************************************
    
    // **********************************************
    // Class Methods
    // **********************************************
    
    // **********************************************
    // Instance Methods
    // **********************************************
    
    @Override
    public void end(boolean interrupted) {
        // TODO Auto-generated method stub
        super.end(interrupted);
        System.out.println(String.format("Entering %s::%s", this.getClass().getSimpleName(), new Throwable().getStackTrace()[0].getMethodName()));
        
    }

    @Override
    public void execute() {
        System.out.println(String.format("Entering %s::%s", this.getClass().getSimpleName(), new Throwable().getStackTrace()[0].getMethodName()));
        
        //climber.stopAtLimit();
    }

    @Override
    public void initialize() {
        System.out.println(String.format("Entering %s::%s", this.getClass().getSimpleName(), new Throwable().getStackTrace()[0].getMethodName()));
    }

    @Override
    public boolean isFinished() {
        return true;

    }
    
}
