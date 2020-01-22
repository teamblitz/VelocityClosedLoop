/*
 * Phoenix Software License Agreement
 *
 * Copyright (C) Cross The Road Electronics.  All rights
 * reserved.
 * 
 * Cross The Road Electronics (CTRE) licenses to you the right to 
 * use, publish, and distribute copies of CRF (Cross The Road) firmware files (*.crf) and 
 * Phoenix Software API Libraries ONLY when in use with CTR Electronics hardware products
 * as well as the FRC roboRIO when in use in FRC Competition.
 * 
 * THE SOFTWARE AND DOCUMENTATION ARE PROVIDED "AS IS" WITHOUT
 * WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT
 * LIMITATION, ANY WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE, TITLE AND NON-INFRINGEMENT. IN NO EVENT SHALL
 * CROSS THE ROAD ELECTRONICS BE LIABLE FOR ANY INCIDENTAL, SPECIAL, 
 * INDIRECT OR CONSEQUENTIAL DAMAGES, LOST PROFITS OR LOST DATA, COST OF
 * PROCUREMENT OF SUBSTITUTE GOODS, TECHNOLOGY OR SERVICES, ANY CLAIMS
 * BY THIRD PARTIES (INCLUDING BUT NOT LIMITED TO ANY DEFENSE
 * THEREOF), ANY CLAIMS FOR INDEMNITY OR CONTRIBUTION, OR OTHER
 * SIMILAR COSTS, WHETHER ASSERTED ON THE BASIS OF CONTRACT, TORT
 * (INCLUDING NEGLIGENCE), BREACH OF WARRANTY, OR OTHERWISE
 */

/**
 * Description:
 * The VelocityClosedLoop example demonstrates the velocity closed-loop servo.
 * Tested with Logitech F350 USB Gamepad inserted into Driver Station]
 * 
 * Be sure to select the correct feedback sensor using configSelectedFeedbackSensor() below.
 * Use Percent Output Mode (Holding A and using Left Joystick) to confirm talon is driving 
 * forward (Green LED on Talon/Victor) when the postion sensor is moving in the postive 
 * direction. If this is not the case, flip the boolean input in setSensorPhase().
 * 
 * Controls:
 * Button 1: When held, start and run Velocity Closed Loop on Talon/Victor
 * Left Joystick Y-Axis:
 * 	+ Percent Output: Throttle Talon forward and reverse, use to confirm hardware setup
 * 	+ Velocity Closed Loop: Servo Talon forward and reverse [-500, 500] RPM
 * 
 * Gains for Velocity Closed Loop may need to be adjusted in Constants.java
 * 
 * Supported Version:
 * - Talon SRX: 4.00
 * - Victor SPX: 4.00
 * - Pigeon IMU: 4.00
 * - CANifier: 4.00
 */
package frc.robot;

import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInWidgets;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.wpilibj.Joystick;

import com.ctre.phoenix.motorcontrol.can.*;
import com.ctre.phoenix.motorcontrol.*;

public class Robot extends TimedRobot {
    /* Hardware */
	TalonSRX _talon_top = new TalonSRX(2);
	TalonSRX _talon_bottom = new TalonSRX(3);
    Joystick _joy = new Joystick(0);
    
    /* String for output */
    StringBuilder _sb = new StringBuilder();
    
    /* Loop tracker for prints */
	int _loops = 0;

	private ShuffleboardTab speedcontrols = Shuffleboard.getTab("Controls");

	private NetworkTableEntry topMotorEntry = Shuffleboard.getTab("Controls")
		.add("Top Motor", _talon_top.getSelectedSensorVelocity(Constants.kPIDLoopIdx))
		.withWidget(BuiltInWidgets.kTextView)
		.getEntry();
		
	private NetworkTableEntry bottomMotorEntry = Shuffleboard.getTab("Controls")
		.add("Bottom Motor", _talon_bottom.getSelectedSensorVelocity(Constants.kPIDLoopIdx))
		.withWidget(BuiltInWidgets.kTextView)
		.getEntry();

	public void robotInit() {
        /* Factory Default all hardware to prevent unexpected behaviour */
		_talon_top.configFactoryDefault();
		_talon_bottom.configFactoryDefault();


		/* Config sensor used for Primary PID [Velocity] */
        _talon_top.configSelectedFeedbackSensor(FeedbackDevice.CTRE_MagEncoder_Relative,
                                            Constants.kPIDLoopIdx, 
											Constants.kTimeoutMs);
		_talon_bottom.configSelectedFeedbackSensor(FeedbackDevice.CTRE_MagEncoder_Relative,
                                            Constants.kPIDLoopIdx, 
                                            Constants.kTimeoutMs);

        /**
		 * Phase sensor accordingly. 
         * Positive Sensor Reading should match Green (blinking) Leds on Talon
         */
		_talon_top.setSensorPhase(true);

		/* Config the peak and nominal outputs */
		_talon_top.configNominalOutputForward(0, Constants.kTimeoutMs);
		_talon_top.configNominalOutputReverse(0, Constants.kTimeoutMs);
		_talon_top.configPeakOutputForward(1, Constants.kTimeoutMs);
		_talon_top.configPeakOutputReverse(-1, Constants.kTimeoutMs);

		/* Config the Velocity closed loop gains in slot0 */
		_talon_top.config_kF(Constants.kPIDLoopIdx, Constants.kGains_Velocit.kF, Constants.kTimeoutMs);
		_talon_top.config_kP(Constants.kPIDLoopIdx, Constants.kGains_Velocit.kP, Constants.kTimeoutMs);
		_talon_top.config_kI(Constants.kPIDLoopIdx, Constants.kGains_Velocit.kI, Constants.kTimeoutMs);
		_talon_top.config_kD(Constants.kPIDLoopIdx, Constants.kGains_Velocit.kD, Constants.kTimeoutMs);
		
		_talon_bottom.setSensorPhase(true);

		/* Config the peak and nominal outputs */
		_talon_bottom.configNominalOutputForward(0, Constants.kTimeoutMs);
		_talon_bottom.configNominalOutputReverse(0, Constants.kTimeoutMs);
		_talon_bottom.configPeakOutputForward(1, Constants.kTimeoutMs);
		_talon_bottom.configPeakOutputReverse(-1, Constants.kTimeoutMs);

		/* Config the Velocity closed loop gains in slot0 */
		_talon_bottom.config_kF(Constants.kPIDLoopIdx, Constants.kGains_Velocit.kF, Constants.kTimeoutMs);
		_talon_bottom.config_kP(Constants.kPIDLoopIdx, Constants.kGains_Velocit.kP, Constants.kTimeoutMs);
		_talon_bottom.config_kI(Constants.kPIDLoopIdx, Constants.kGains_Velocit.kI, Constants.kTimeoutMs);
		_talon_bottom.config_kD(Constants.kPIDLoopIdx, Constants.kGains_Velocit.kD, Constants.kTimeoutMs);
	}

	/**
	 * This function is called periodically during operator control
	 */
	public void teleopPeriodic() {
		/* Get gamepad axis */

		
		double leftYstick = -1 * _joy.getY();

		/* Get Talon/Victor's current output percentage */
		double motorOutput_top = _talon_top.getMotorOutputPercent();
		double motorOutput_bottom = _talon_bottom.getMotorOutputPercent();

		
		/* Prepare line to print */
		_sb.append("\tout:");
		/* Cast to int to remove decimal places */
		_sb.append((int) (motorOutput_top * 100));
		_sb.append((int) (motorOutput_bottom * 100));
		_sb.append("%");	// Percent

		_sb.append("\tspd:");
		_sb.append(_talon_top.getSelectedSensorVelocity(Constants.kPIDLoopIdx));
		_sb.append(_talon_bottom.getSelectedSensorVelocity(Constants.kPIDLoopIdx));
		_sb.append("u"); 	// Native units

        /** 
		 * When button 1 is held, start and run Velocity Closed loop.
		 * Velocity Closed Loop is controlled by joystick position x500 RPM, [-500, 500] RPM
		 */
		//if (_joy.getRawButton(1)) {
			/* Velocity Closed Loop */

			/**
			 * Convert 500 RPM to units / 100ms.
			 * 4096 Units/Rev * 500 RPM / 600 100ms/min in either direction:
			 * velocity setpoint is in units/100ms
			 */
			double targetVelocity_UnitsPer100ms = topMotorEntry.getDouble(1.0) * 4096 / 600;
			/* 500 RPM in either direction */
			_talon_top.set(ControlMode.Velocity, targetVelocity_UnitsPer100ms);
			_talon_bottom.set(ControlMode.Velocity, targetVelocity_UnitsPer100ms);

			/* Append more signals to print when in speed mode. */
			_sb.append("\terr:");
			_sb.append(_talon_top.getClosedLoopError(Constants.kPIDLoopIdx));
			_sb.append(_talon_bottom.getClosedLoopError(Constants.kPIDLoopIdx));
			_sb.append("\ttrg:");
			_sb.append(targetVelocity_UnitsPer100ms);
		//} else {
			/* Percent Output */

		//	_talon_top.set(ControlMode.PercentOutput, leftYstick);
		//	_talon_bottom.set(ControlMode.PercentOutput, leftYstick);

		//}

        /* Print built string every 10 loops */
		if (++_loops >= 10) {
			_loops = 0;
			System.out.println(_sb.toString());
        }
        /* Reset built string */
		_sb.setLength(0);

		System.out.println(topMotorEntry.getDouble(1.0));

		}
	}

