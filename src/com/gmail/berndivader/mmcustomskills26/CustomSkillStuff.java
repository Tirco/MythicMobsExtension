package com.gmail.berndivader.mmcustomskills26;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import io.lumine.xikage.mythicmobs.MythicMobs;
import io.lumine.xikage.mythicmobs.adapters.AbstractEntity;
import io.lumine.xikage.mythicmobs.adapters.AbstractLocation;
import io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter;
import io.lumine.xikage.mythicmobs.mobs.ActiveMob;
import io.lumine.xikage.mythicmobs.skills.SkillCaster;
import io.lumine.xikage.mythicmobs.skills.SkillTrigger;
import io.lumine.xikage.mythicmobs.skills.TriggeredSkill;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;

@SuppressWarnings("deprecation")
public class CustomSkillStuff implements Listener {
	
	@EventHandler
	public void RemoveFallingBlockProjectile(EntityChangeBlockEvent e) {
		if (e.getEntity().hasMetadata(Main.mpNameVar)) {
			e.setCancelled(true);
		}
	}
	
	@EventHandler
	public void mmTriggerOnKill(EntityDeathEvent e) {
        EntityDamageEvent entityDamageEvent = e.getEntity().getLastDamageCause();
        if (entityDamageEvent != null 
        		&& !entityDamageEvent.isCancelled() 
        		&& entityDamageEvent instanceof EntityDamageByEntityEvent) {
        	LivingEntity damager = getAttacker(((EntityDamageByEntityEvent)entityDamageEvent).getDamager());
        	if (damager!=null && MythicMobs.inst().getMobManager().isActiveMob(damager.getUniqueId())) {
                new TriggeredSkill(SkillTrigger.KILL, MythicMobs.inst().getMobManager().getMythicMobInstance(damager),
                		BukkitAdapter.adapt(e.getEntity()));
        	}
        }
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onMythicCustomRPGItemDamage(EntityDamageByEntityEvent e) {
		LivingEntity victim = null;
		if (e.getEntity() instanceof LivingEntity) victim = (LivingEntity) e.getEntity();
		if (victim==null || !victim.hasMetadata("MythicDamage")) return;
		if (victim.getMetadata("mmrpgitemdmg").get(0).asBoolean()) {
			victim.removeMetadata("MythicDamage", Main.getPlugin());
			onEntityDamageTaken(e, victim);
		}
	}
	
	@EventHandler
	public void onMythicCustomDamage(EntityDamageByEntityEvent e) {
		LivingEntity victim = null;
		if (e.getEntity() instanceof LivingEntity) victim = (LivingEntity) e.getEntity();
		if (victim==null || !victim.hasMetadata("MythicDamage")) return;
		if (!victim.getMetadata("mmrpgitemdmg").get(0).asBoolean()) {
			victim.removeMetadata("MythicDamage", Main.getPlugin());
			onEntityDamageTaken(e, victim);
		}
	}
	
	private static void onEntityDamageTaken(EntityDamageByEntityEvent e, LivingEntity victim) {
		boolean ignoreArmor = victim.getMetadata("IgnoreArmor").get(0).asBoolean();
		boolean ignoreAbs = victim.getMetadata("IgnoreAbs").get(0).asBoolean();
		boolean debug = victim.getMetadata("mmcdDebug").get(0).asBoolean();
		if (debug) Bukkit.getLogger().info("CustomDamage cancelled? " + Boolean.toString(e.isCancelled()));
		if (e.isCancelled()) return;
		double md = victim.getMetadata("DamageAmount").get(0).asDouble();
		double df = md / e.getDamage(DamageModifier.BASE);
		if (debug) {
			Bukkit.getLogger().info("Orignal BukkitDamage: " + Double.toString(e.getDamage(DamageModifier.BASE)));
			Bukkit.getLogger().info("Custom MythicDamage.: " + Double.toString(md));
			Bukkit.getLogger().info("DamageFactor: " + Double.toString(df));
			Bukkit.getLogger().info("-----------------------------");
		}
		e.setDamage(DamageModifier.BASE, md);
		double damage = e.getDamage(DamageModifier.BASE);
		for (DamageModifier modifier : DamageModifier.values()) {
			if (!e.isApplicable(modifier) || modifier.equals(DamageModifier.BASE)) continue;
			double modF = df;
			if ((modifier.equals(DamageModifier.ARMOR) && ignoreArmor) 
					|| (modifier.equals(DamageModifier.ABSORPTION) && ignoreAbs)) modF = 0D;
			e.setDamage(modifier, modF * e.getDamage(modifier));
			damage+=e.getDamage(modifier);
		}
		if (victim.getMetadata("PreventKnockback").get(0).asBoolean()) {
			e.setCancelled(true);
			victim.damage(damage);
		}
		if (debug) {
			Bukkit.getLogger().info("Finaldamage amount after modifiers: " + Double.toString(damage));
		}
		
	}

	public static void doDamage(SkillCaster am, AbstractEntity t, double damage, 
			boolean ignorearmor, 
			boolean preventKnockback, 
			boolean preventImmunity, 
			boolean ignoreabs,
			boolean debug) {
        LivingEntity target;
        am.setUsingDamageSkill(true);
        if (am instanceof ActiveMob) ((ActiveMob)am).setLastDamageSkillAmount(damage);
        LivingEntity source = (LivingEntity)BukkitAdapter.adapt(am.getEntity());
        target = (LivingEntity)BukkitAdapter.adapt(t);
        target.setMetadata("IgnoreArmor", new FixedMetadataValue(Main.getPlugin(),ignorearmor));
        target.setMetadata("PreventKnockback", new FixedMetadataValue(Main.getPlugin(),preventKnockback));
        target.setMetadata("IgnoreAbs", new FixedMetadataValue(Main.getPlugin(),ignoreabs));
        target.setMetadata("MythicDamage", new FixedMetadataValue(Main.getPlugin(),true));
        target.setMetadata("mmcdDebug", new FixedMetadataValue(Main.getPlugin(),debug));
        target.setMetadata("mmrpgitemdmg", new FixedMetadataValue(Main.getPlugin(),false));
		if (!ignorearmor && Main.hasRpgItems && target instanceof Player) {
			damage=rpgItemPlayerHit((Player)target, damage);
		}
		if (Math.abs(damage)<0.01) damage=0.01;
        target.setMetadata("DamageAmount", new FixedMetadataValue(Main.getPlugin(),damage));
		target.damage(damage, source);
	    if (preventImmunity) target.setNoDamageTicks(0);
	    am.setUsingDamageSkill(false);
	}
	
	public static LivingEntity getAttacker(Entity damager) {
        if (damager instanceof Projectile) {
            if (((Projectile)damager).getShooter() instanceof LivingEntity) {
                LivingEntity shooter = (LivingEntity)((Projectile)damager).getShooter();
                if (shooter != null && shooter instanceof LivingEntity) {
                    return shooter;
                }
            } else {
                return null;
            }
        }
        if (damager instanceof LivingEntity) {
            return (LivingEntity)damager;
        }
        return null;
	}
	
    public static Location getLocationInFront(Location start, double range) {
    	Location l = start.clone().add(start.getDirection().setY(0).normalize().multiply(range));
        return l;
    }
    
    public static double rpgItemPlayerHit(Player p, double damage) {
        ItemStack[] armour = p.getInventory().getArmorContents();
        boolean useDamage=false;
        for (ItemStack pArmour : armour) {
            RPGItem pRItem = ItemManager.toRPGItem(pArmour);
            if (pRItem == null) continue;
            boolean can;
            if (!pRItem.hitCostByDamage) {
                can = pRItem.consumeDurability(pArmour, pRItem.hitCost);
            } else {
                can = pRItem.consumeDurability(pArmour, (int) (pRItem.hitCost * damage / 100d));
            }
            if (can && pRItem.getArmour() > 0) {
            	useDamage=true;
                damage -= Math.round(damage * (((double) pRItem.getArmour()) / 100d));
            }
        }
        if (useDamage) p.setMetadata("mmrpgitemdmg", new FixedMetadataValue(Main.getPlugin(),useDamage));
        return damage;
    }    
    
    public static LivingEntity getTargetedEntity(Player player) {
    	int range = 32;
    	BlockIterator bi;
    	List<Entity> ne = player.getNearbyEntities(range, range, range);
    	List<LivingEntity> entities = new ArrayList<LivingEntity>();
    	for (Entity en : ne) {
			if ((en instanceof LivingEntity) && !en.hasMetadata(Main.noTargetVar)) {
    			entities.add((LivingEntity)en);
    		}
    	}
    	LivingEntity target = null;
    	bi = new BlockIterator(player, range);
    	int bx;
    	int by;
    	int bz;
    	while (bi.hasNext()) {
    		Block b = bi.next();
    		bx = b.getX();
    		by = b.getY();
    		bz = b.getZ();
    		if (!b.getType().isTransparent()) break;
    		for (LivingEntity e : entities) {
    			Location l = e.getLocation();
    			double ex = l.getX();
    			double ey = l.getY();
    			double ez = l.getZ();
    			if ((bx - 0.75D <= ex) && (ex <= bx + 1.75D) && (bz - 0.75D <= ez) && (ez <= bz + 1.75D) && (by - 1 <= ey) && (ey <= by + 2.5D)) {
    				target = e;
    				if ((target != null) && ((target instanceof Player)) && (((Player)target).getGameMode() == org.bukkit.GameMode.CREATIVE)) {
    					target = null;
    				} else {
    					return target;
    				}
    			}
    		}
    	}
    	return null;
   	}

	public static void applyInvisible(LivingEntity le, long runlater) {
		PotionEffect pe = new PotionEffect(PotionEffectType.INVISIBILITY, 2073600, 4, false, false);
		pe.apply(le);
		new BukkitRunnable() {
			@Override
			public void run() {
				le.getEquipment().clear();
			}
		}.runTaskLater(Main.getPlugin(), runlater);
	}
	
    public static final float DEGTORAD = 0.017453293F;
    public static final float RADTODEG = 57.29577951F;
    
    public static float getLookAtYaw(Entity loc, Entity lookat) {
        return getLookAtYaw(loc.getLocation(), lookat.getLocation());
    }
    
    public static float getLookAtYaw(Block loc, Block lookat) {
        return getLookAtYaw(loc.getLocation(), lookat.getLocation());
    }
    
    public static float getLookAtYaw(Location loc, Location lookat) {
        return getLookAtYaw(lookat.getX() - loc.getX(), lookat.getZ() - loc.getZ());
    }
    
    public static float getLookAtYaw(Vector motion) {
        return getLookAtYaw(motion.getX(), motion.getZ());
    }
    
    public static float getLookAtYaw(double dx, double dz) {
        float yaw = 0;
        if (dx != 0) {
            if (dx < 0) {
                yaw = 270;
            } else {
                yaw = 90;
            }
            yaw -= atan(dz / dx);
        } else if (dz < 0) {
            yaw = 180;
        }
        return -yaw - 90;
    }
    
    private static float atan(double value) {
        return RADTODEG * (float) Math.atan(value);
    }

    public static Location move(Location loc, Vector offset) {
        return move(loc, offset.getX(), offset.getY(), offset.getZ());
    }
    
    public static Location move(Location loc, double dx, double dy, double dz) {
        Vector off = rotate(loc.getYaw(), loc.getPitch(), dx, dy, dz);
        double x = loc.getX() + off.getX();
        double y = loc.getY() + off.getY();
        double z = loc.getZ() + off.getZ();
        return new Location(loc.getWorld(), x, y, z, loc.getYaw(), loc.getPitch());
    }
    
    public static Vector rotate(float yaw, float pitch, Vector value) {
        return rotate(yaw, pitch, value.getX(), value.getY(), value.getZ());
    }
    
    public static Vector rotate(float yaw, float pitch, double x, double y, double z) {
        float angle;
        angle = yaw * DEGTORAD;
        double sinyaw = Math.sin(angle);
        double cosyaw = Math.cos(angle);
        angle = pitch * DEGTORAD;
        double sinpitch = Math.sin(angle);
        double cospitch = Math.cos(angle);
        double newx = 0.0;
        double newy = 0.0;
        double newz = 0.0;
        newz -= x * cosyaw;
        newz -= y * sinyaw * sinpitch;
        newz -= z * sinyaw * cospitch;
        newx += x * sinyaw;
        newx -= y * cosyaw * sinpitch;
        newx -= z * cosyaw * cospitch;
        newy += y * cospitch;
        newy -= z * sinpitch;
        return new Vector(newx, newy, newz);
    }

	public static float lookAt(Location loc, Location lookat) {
        loc = loc.clone();
        lookat = lookat.clone();
        float yaw=0.0F;
        double dx = lookat.getX() - loc.getX();
        double dy = lookat.getY() - loc.getY();
        double dz = lookat.getZ() - loc.getZ();
        
        if (dx != 0) {
            if (dx < 0) {
                yaw=(float)(1.5 * Math.PI);
            } else {
                yaw=(float)(0.5*Math.PI);
            }
            yaw=yaw-(float)Math.atan(dz/dx);
        } else if (dz < 0) {
            yaw=(float)Math.PI;
        }
        double dxz = Math.sqrt(Math.pow(dx, 2) + Math.pow(dz, 2));
//        loc.setPitch((float) -Math.atan(dy / dxz));
        yaw=-yaw*180f/(float)Math.PI;
//        loc.setPitch(loc.getPitch() * 180f / (float) Math.PI);
        return yaw;
    }
	
    public static Location moveTo(Location loc, Vector offset) {
        float ryaw = -loc.getYaw() / 180f * (float) Math.PI;
        float rpitch = loc.getPitch() / 180f * (float) Math.PI;
        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();
        z -= offset.getX() * Math.sin(ryaw);
        z += offset.getY() * Math.cos(ryaw) * Math.sin(rpitch);
        z += offset.getZ() * Math.cos(ryaw) * Math.cos(rpitch);
        x += offset.getX() * Math.cos(ryaw);
        x += offset.getY() * Math.sin(rpitch) * Math.sin(ryaw);
        x += offset.getZ() * Math.sin(ryaw) * Math.cos(rpitch);
        y += offset.getY() * Math.cos(rpitch);
        y -= offset.getZ() * Math.sin(rpitch);
        return new Location(loc.getWorld(), x, y, z, loc.getYaw(), loc.getPitch());
    }	
    
	public static AbstractLocation getCircleLoc(Location c, double rX, double rZ, double rY, double air) {
        double x = c.getX() + rX * Math.cos(air);
        double z = c.getZ() + rZ * Math.sin(air);
        double y = c.getY() + rY * Math.cos(air);
        Location loc = new Location(c.getWorld(), x, y, z);
        Vector difference = c.toVector().clone().subtract(loc.toVector()); 
        loc.setDirection(difference);
        return BukkitAdapter.adapt(loc);
    }	    
}