package frc.robot;

import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;

/**
 * TimedRobot shell. All wiring lives in {@link RobotContainer}; this class just
 * runs the {@link CommandScheduler} and hands off auto/teleop.
 */
public class Robot extends TimedRobot {

    private Command autonomousCommand;
    private final RobotContainer container = new RobotContainer();

    @Override
    public void robotPeriodic() {
        // Runs every mechanism's periodic() and simulationPeriodic(), plus the
        // SuperstructureCoordinator + GoalDirector commands.
        CommandScheduler.getInstance().run();
    }

    @Override
    public void simulationPeriodic() {
        // Services the browser Sim Cockpit (localhost:5805) and auto-enables.
        container.simPeriodic();
    }

    @Override
    public void autonomousInit() {
        autonomousCommand = container.getAutonomousCommand();
        if (autonomousCommand != null) {
            CommandScheduler.getInstance().schedule(autonomousCommand);
        }
    }

    @Override
    public void teleopInit() {
        if (autonomousCommand != null) {
            CommandScheduler.getInstance().cancel(autonomousCommand);
        }
    }
}
