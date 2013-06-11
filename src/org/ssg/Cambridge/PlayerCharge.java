package org.ssg.Cambridge;

import net.java.games.input.Controller;

import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.geom.Polygon;
import org.newdawn.slick.geom.Transform;

import paulscode.sound.SoundSystem;
import paulscode.sound.SoundSystemConfig;

public class PlayerCharge extends Player{

	//The vector between Charge and the ball, divided into components || and normal to Charge's velocity
	float[] ballParallel, ballOrth;
	float TRAILRANGE;
	
	Polygon poly;
	//Theta is between -pi and pi, theta2 is 0 to 2pi
	float theta2, thetaTarget, thetaTarget2;
	boolean buttonPressed;
	
	Ball ball;
	
	public PlayerCharge(int n, float[] consts, int[] f, int[] c, Controller c1, boolean c1Exist, float[] p, int[] xyL, Color se, SoundSystem ss, String sn, Ball b) {
		super(n, consts, f, c, c1, c1Exist, p, xyL, se, ss, sn);

		MAXPOWER = 100;
		TRAILRANGE = KICKRANGE+50;

		poly = new Polygon(new float[]{0,0,-PLAYERSIZE/3, -PLAYERSIZE/2, PLAYERSIZE*2/3, 0, -PLAYERSIZE/3, PLAYERSIZE/2});
		ballParallel = new float[2];
		ballOrth = new float[2];
		theta2 = theta;
		thetaTarget = theta;
		thetaTarget2 = theta;
		
		buttonPressed = false;
		
		ball = b;
	}

	//Show if stick is being held down during a dash charge
	@Override
	public void drawKickCircle(Graphics g){
		//Draw kicking circle
		if(power>0){
			g.setColor(getColor(.2f+mag(vel)/VELMAG/2f));
		}else{
			g.setColor(getColor(.5f).darker());
		}
		g.drawOval(getX()-getKickRange()/2, getY()-getKickRange()/2, getKickRange(), getKickRange());
		//Kicking circle flash when kick happens
		g.setColor(getColor2().brighter());
		g.drawOval(getX()-getKickRange()/2f, getY()-getKickRange()/2f, getKickRange(), getKickRange());
	}
	
	@Override
	public void drawPlayer(Graphics g){
		g.setColor(getColor());
		g.setLineWidth(2f);
		g.translate(pos[0],pos[1]);
		poly = (Polygon) poly.transform(Transform.createRotateTransform(theta));
		g.draw(poly);
		poly = (Polygon) poly.transform(Transform.createRotateTransform(-theta));
		g.translate(-pos[0], -pos[1]);
		g.setLineWidth(5f);
		
		//Debugging
//		g.setColor(getColor());
//		g.drawLine(pos[0], pos[1], pos[0]+ballParallel[0], pos[1]+ballParallel[1]);
//		g.drawLine(pos[0], pos[1], pos[0]+ballOrth[0], pos[1]+ballOrth[1]);
		
	}
	
	@Override
	public void update(int delta) {

		if (cExist) {
			pollController(delta);
			
			if (actionButton.getPollData() == 1.0){
				if(!buttonPressed){
					activatePower();
					buttonPressed = true;
				}
			}else if(buttonPressed){
					powerKeyReleased();
					buttonPressed = false;
			}
		
		}

		updatePos(delta);

		lastKickAlpha -= (float)(delta)/2400f;
		if(lastKickAlpha<0){
			lastKickAlpha = 0f;
		}
		
		kickingCoolDown -= (float)delta;
		if(kickingCoolDown<0)
			kickingCoolDown = 0;
		
		if(power>0){
			power -= (float)delta/12f;
			if(power<=0){
				powerKeyReleased();
				
				if(mag(vel)>0){
					mySoundSystem.quickPlay( true, "pow2.wav", false, 0, 0, 0, SoundSystemConfig.ATTENUATION_NONE, 0.0f );
					//In this case commandeered to draw the dash trail
					setLastKick(pos[0],pos[1],pos[0]+vel[0],pos[1]+vel[1],1f);
					
					parallelComponent(new float[] {ball.getX()-pos[0], ball.getY()-pos[1]}, vel, ballParallel);
					ballOrth[0] = ball.getX()-pos[0]-ballParallel[0];
					ballOrth[1] = ball.getY()-pos[1]-ballParallel[1];
					
					if(mag(ballOrth)<TRAILRANGE/2){
						if(sameDir(vel,ballParallel)){//Push it aside
							tempf = mag(ballOrth)/(TRAILRANGE/2);//goes from 0 to 1, small when ball is close to center line
							ball.setVel(new float[]{2f*ballOrth[0]-tempf*ballParallel[0],2f*ballOrth[1]-tempf*ballParallel[1]}, 1.2f*POWERKICK);
							ball.setAcc(new float[]{-ballParallel[0],-ballParallel[1]}, 1f-tempf);
						}else{//If it's behind you, backkick relative to distance
							ball.setVel(new float[]{ballParallel[0], ballParallel[1]}, 2f*POWERKICK);
						}
						ball.setLastKicker(playerNum);
						ball.setCanBeKicked(playerNum, false);
						kickingCoolDown = KICKCOOLDOWN;
					}
					
					while(pos[0]-KICKRANGE/2>=xyLimit[0]&&pos[0]+KICKRANGE/2<=xyLimit[1]&&pos[1]-KICKRANGE/2>=xyLimit[2]&&pos[1]+KICKRANGE/2<=xyLimit[3]){
						pos[0]+=vel[0];
						pos[1]+=vel[1];
					}
					
					if(pos[0]<xyLimit[0]+KICKRANGE/2)
						pos[0]=xyLimit[0]+KICKRANGE/2;
					if(pos[0]>xyLimit[1]-KICKRANGE/2)
						pos[0]=xyLimit[1]-KICKRANGE/2;
					if(pos[1]<xyLimit[2]+KICKRANGE/2)
						pos[1]=xyLimit[2]+KICKRANGE/2;
					if(pos[1]>xyLimit[3]-KICKRANGE/2)
						pos[1]=xyLimit[3]-KICKRANGE/2;
					
				}
			}
		}
		
		//Angle code
		if(power>0){
			if(mag(vel)!=0)
				thetaTarget = (float)Math.atan2(vel[1],vel[0]);
			
			theta2 = theta;
			if(theta2<0)
				theta2 += 2f*(float)Math.PI;
			thetaTarget2 = thetaTarget;
			if(thetaTarget2<0)
				thetaTarget2 += 2f*(float)Math.PI;
	
			//Choose the direction of shortest rotation
			if(Math.abs(thetaTarget-theta)-Math.abs(thetaTarget2-theta2) >= 0){
				theta = approachTarget(theta2, thetaTarget2, (float)delta/120f);
			}else{
				theta = approachTarget(theta, thetaTarget, (float)delta/120f);
			}
			
			//Set theta between -pi and pi, for the next round of calculation
			if(theta>(float)Math.PI)
				theta-=(float)Math.PI*2f;
			
		}else{
			theta += omega*(float)delta/60f*Math.PI;
			if(theta>2f*(float)Math.PI)
				theta-=2f*(float)Math.PI;
		}
	}
	
	@Override
	public void activatePower() {
		power = MAXPOWER;
		velMag = 0;
		mySoundSystem.quickPlay( true, "whoosh2r.wav", false, 0, 0, 0, SoundSystemConfig.ATTENUATION_NONE, 0.0f );
	}

	@Override
	public void powerKeyReleased() {
		power = 0;
		velMag = VELMAG;
	}

	@Override
	public boolean isKicking() {
		return true;
	}

	@Override
	public boolean flashKick() {
		return false;
	}

	@Override
	public void setPower() {

	}
	
	//Parallel component of u on v, written to w
	public void parallelComponent(float[] u, float[] v, float[] w){
		tempf = (u[0]*v[0]+u[1]*v[1])/mag(v)/mag(v);
		w[0] = v[0]*tempf;
		w[1] = v[1]*tempf;		
	}
	
	//vx is vel, dir is ballParallel
	public boolean sameDir(float[] vx, float[] dir){
		if(mag(vx)==0 || mag(dir) == 0)//Unsure about this, should never be called though
			return false;
		if(vx[0]==0 && vx[1]!=0)
			return vx[1]/Math.abs(vx[1]) == dir[1]/Math.abs(dir[1]);
		if(vx[1]==0 && vx[0]!=0)
			return vx[0]/Math.abs(vx[0]) == dir[0]/Math.abs(dir[0]);
		return vx[0]/Math.abs(vx[0]) == dir[0]/Math.abs(dir[0]) && vx[1]/Math.abs(vx[1])==dir[1]/Math.abs(dir[1]);
	}
	
}
