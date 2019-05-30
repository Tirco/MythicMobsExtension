package com.gmail.berndivader.mythicmobsext.volatilecode.v1_13_R2.pathfindergoals;

import java.util.ArrayList;
import java.util.Optional;

import net.minecraft.server.v1_13_R2.BlockPosition;
import net.minecraft.server.v1_13_R2.EntityInsentient;
import net.minecraft.server.v1_13_R2.EnumBlockFaceShape;
import net.minecraft.server.v1_13_R2.EnumDirection;
import net.minecraft.server.v1_13_R2.IBlockData;
import net.minecraft.server.v1_13_R2.Navigation;
import net.minecraft.server.v1_13_R2.NavigationAbstract;
import net.minecraft.server.v1_13_R2.NavigationFlying;
import net.minecraft.server.v1_13_R2.PathType;
import net.minecraft.server.v1_13_R2.PathfinderGoal;
import net.minecraft.server.v1_13_R2.Vec3D;

import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftEntity;
import org.bukkit.event.entity.EntityTeleportEvent;

import com.gmail.berndivader.mythicmobsext.utils.TravelPoints;
import com.gmail.berndivader.mythicmobsext.utils.Utils;

import io.lumine.xikage.mythicmobs.mobs.ActiveMob;

public
class 
PathfinderGoalTravelAround 
extends
PathfinderGoal
{
	
	ArrayList<Vec3D>travelpoints;
	
	private final EntityInsentient d;
	private Vec3D v;
	private Optional<ActiveMob> mM;
	private Vec3D aV;
	private final double f;
	private final double mR,tR;
	net.minecraft.server.v1_13_R2.World a;
	private final NavigationAbstract g;
	private int h;
	float b;
	float c;
	private float i;
	private boolean iF,iT;
	
	public PathfinderGoalTravelAround(EntityInsentient entity,double d0,double mR,double tR,boolean iT) {
		this.d=entity;
		this.f=d0;
		this.a=entity.world;
		g=entity.getNavigation();
		this.travelpoints=new ArrayList<>();
		this.v=nextCheckPoint();
		a(3);
		this.mR=mR;
		this.tR=tR;
		this.iF=false;
		this.iT=iT;
		if ((!(entity.getNavigation() instanceof Navigation)) && (!(entity.getNavigation() instanceof NavigationFlying))) {
			throw new IllegalArgumentException("Unsupported mob type for TravelAroundGoal");
		}
		this.mM=Utils.mobmanager.getActiveMob(entity.getUniqueID());
		TravelPoints.travelpoints.put(d.getUniqueID(),new ArrayList<com.gmail.berndivader.mythicmobsext.utils.Vec3D>());
	}

	@Override
	public boolean a() {
		this.aV=new Vec3D(d.locX,d.locY,d.locZ);
		if(this.v!=null) {
			if (this.iT||this.d.getGoalTarget()==null||!this.d.getGoalTarget().isAlive()) {
				double ds=v.distanceSquared(this.aV);
				if (ds>this.mR) {
					return true;
				} else if (this.iF&&ds>2.0D) {
					return true;
				} else {
					this.v=nextCheckPoint();
				}
			}
		} else {
			this.v=nextCheckPoint();
		}
		return false;
	}
	
	@Override
	public boolean b() {
		return (!g.p())&&v.distanceSquared(this.aV)>2.0D;
	}
	
	@Override
	public void c() {
		h=0;
		i=d.a(PathType.WATER);
		d.a(PathType.WATER,0.0F);
		if(!this.iF) {
			if(this.mM.isPresent()) {
				ActiveMob am=this.mM.get();
				am.signalMob(null,"GOAL_TRAVELSTART");
			}
		}
		this.iF=true;
	}
  
	@Override
	public void d() {
		g.p();
		d.a(PathType.WATER, i);
		if (v.distanceSquared(this.aV)<10.0D) {
			this.iF=false;
			this.v=null;
			if (this.mM.isPresent()) {
				ActiveMob am=this.mM.get();
				am.signalMob(null,"GOAL_TRAVELEND");
			}
		}
	}
	
	@Override
	public void e() {
		d.getControllerLook().a(v.x,v.y,v.z,10.0F,d.K());
		if (h--<=0) {
			h=10;
			if (!g.a(v.x,v.y,v.z,f)&&(!d.isLeashed())&&(!d.isPassenger())&&v.distanceSquared(this.aV)>this.tR) {
				CraftEntity entity=d.getBukkitEntity();
				Location to=new Location(entity.getWorld(),v.x,v.y,v.z,d.yaw,d.pitch);
				EntityTeleportEvent event=new EntityTeleportEvent(entity,entity.getLocation(),to);
				d.world.getServer().getPluginManager().callEvent(event);
				if (event.isCancelled()) return;
				to=event.getTo();
				d.setPositionRotation(to.getX(),to.getY(),to.getZ(),to.getYaw(),to.getPitch());
				g.p();
				return;
			}
		}
	}
  
	protected boolean a(int i, int j, int k, int l, int i1) {
		BlockPosition blockposition=new BlockPosition(i+l,k-1,j+i1);
		IBlockData iblockdata=a.getType(blockposition);
		return (iblockdata.c(a,blockposition,EnumDirection.DOWN)==EnumBlockFaceShape.SOLID)&&(iblockdata.a(d))&&(a.isEmpty(blockposition.up()))&&(a.isEmpty(blockposition.up(2)));
	}
	
	protected Vec3D nextCheckPoint() {
		int size=this.travelpoints.size();
		Vec3D vector=null;
		if(size>0) {
			vector=travelpoints.get(size-1);
			travelpoints.remove(size-1);
		}
		return vector;
	}
	
	public void addTravelPoint(com.gmail.berndivader.mythicmobsext.utils.Vec3D vector) {
		travelpoints.add(new Vec3D(vector.getX(),vector.getY(),vector.getZ()));
	}
	
	protected Vec3D nextCheckPoint_old() {
		if(TravelPoints.travelpoints.containsKey(d.getUniqueID())) {
			int size=TravelPoints.travelpoints.get(d.getUniqueID()).size();
			if(TravelPoints.travelpoints.get(d.getUniqueID()).size()>0) {
				com.gmail.berndivader.mythicmobsext.utils.Vec3D vector=TravelPoints.travelpoints.get(d.getUniqueID()).get(size-1);
				TravelPoints.travelpoints.get(d.getUniqueID()).remove(size-1);
				return new Vec3D(vector.getX(),vector.getY()+(double)d.getHeadHeight(),vector.getZ());
			}
		}
		return null;
	}
}
