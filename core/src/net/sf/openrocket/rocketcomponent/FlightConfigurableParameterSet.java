package net.sf.openrocket.rocketcomponent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import net.sf.openrocket.util.ArrayList;
import net.sf.openrocket.util.Utils;

/**
 * Represents a value or parameter that can vary based on the
 * flight configuration ID.
 * <p>
 * The parameter value is always defined, and null is not a valid
 * parameter value.
 *
 * @param <E>	the parameter type
 */
public class FlightConfigurableParameterSet<E extends FlightConfigurableParameter<E>> implements Iterable<E> {
	
	//private static final Logger log = LoggerFactory.getLogger(ParameterSet.class);
	protected final HashMap<FlightConfigurationId, E> map = new HashMap<FlightConfigurationId, E>();
	protected static final FlightConfigurationId defaultValueId = FlightConfigurationId.DEFAULT_VALUE_FCID;
	
	/**
	 * Construct a FlightConfiguration that has no overrides.
	 * 
	 * @param component		the rocket component on which events are fired when the parameter values are changed
	 * @param eventType		the event type that will be fired on changes
	 */
	public FlightConfigurableParameterSet(E _defaultValue) {
		this.map.put( defaultValueId, _defaultValue);
	}
	
	/**
	 * Construct a copy of an existing FlightConfigurationImpl.
	 * 
	 * @param component		the rocket component on which events are fired when the parameter values are changed
	 * @param eventType		the event type that will be fired on changes
	 */
	public FlightConfigurableParameterSet(FlightConfigurableParameterSet<E> configSet ){
		for (FlightConfigurationId key : configSet.map.keySet()) {
			E cloneConfig = configSet.map.get(key).clone();
			this.map.put(key, cloneConfig);
		}
	}
	
	/**
	 * Return the default parameter value for this FlightConfiguration.
	 * This is used in case a per-flight configuration override
	 * has not been defined.
	 * 
	 * @return the default parameter value (never null)
	 */
	public E getDefault(){
		return this.map.get( defaultValueId);
	}
	
	/**
	 * Set the default parameter value for this FlightConfiguration.
	 *This is used in case a per-flight configuration override
	 * has not been defined.
	 *  
	 * @param value		the parameter value (null not allowed)
	 */
	public void setDefault(E nextDefaultValue) {
		if (nextDefaultValue == null) {
			throw new NullPointerException("new Default Value is null");
		}
		if( this.isDefault(nextDefaultValue)){
			return;
		}
		this.map.put( defaultValueId, nextDefaultValue);
	}
	
	@Override
	public Iterator<E> iterator() {
		return map.values().iterator();
	}
	
	/**
	 * Return the number of specific flight configurations other than the default.
	 * @return
	 */
	public int size() {
		return (map.size()-1);
	}

	/**
	 * Return the parameter value for the provided flight configuration ID.
	 * This returns either the value specified for this flight config ID,
	 * or the default value.
	 * 
	 * @param    value the parameter to find
	 * @return   the flight configuration ID
	 */
	public FlightConfigurationId getId(E testValue) {
		if( null == testValue ){
			return null;
		}
		for( Entry<FlightConfigurationId, E> curEntry : this.map.entrySet()){
			FlightConfigurationId curKey = curEntry.getKey();
			E curValue = curEntry.getValue();
			
			if( testValue.equals(curValue)){
				return curKey;
			}
		}
		
		return null;
	}

	public E get(final int index) {
		if( 0 > index){
			throw new ArrayIndexOutOfBoundsException("Attempt to retrieve a configurable parameter by an index less than zero: "+index);
		}
		if(( 0 > index) || ( this.map.size() <= index )){
			throw new ArrayIndexOutOfBoundsException("Attempt to retrieve a configurable parameter with an index larger "
													+" than the stored values: "+index+"/"+this.map.size());
		}
		
		List<FlightConfigurationId> ids = this.getSortedConfigurationIDs();
		FlightConfigurationId selectedId = ids.get(index);
		return this.map.get(selectedId);
	}
	
	/**
	 * Return the parameter value for the provided flight configuration ID.
	 * This returns either the value specified for this flight config ID,
	 * or the default value.
	 * 
	 * @param id	the flight configuration ID
	 * @return		the parameter to use (never null)
	 */
	public E get(FlightConfigurationId id) {
		if( id.hasError() ){
			throw new NullPointerException("Attempted to retrieve a parameter with an error key!");
		}
		E toReturn;
		if (map.containsKey(id)) {
			toReturn = map.get(id);
		} else {
			toReturn = this.getDefault();
		}
		return toReturn;
	}

	/**
	 * @return a sorted list of all the contained FlightConfigurationIDs
	 */
	public List<FlightConfigurationId> getSortedConfigurationIDs(){
		ArrayList<FlightConfigurationId> toReturn = new ArrayList<FlightConfigurationId>(); 
		
		toReturn.addAll( this.map.keySet() );
		toReturn.remove( defaultValueId );
		// Java 1.8:
		//toReturn.sort( null );
		
		// Java 1.7: 
	    Collections.sort(toReturn);
			
		return toReturn;
	}
	
	public List<FlightConfigurationId> getIds(){
		return this.getSortedConfigurationIDs();
	}
    
	/**
	 * Set the parameter value for the provided flight configuration ID.
	 * This sets the override for this flight configuration ID.
	 * 
	 * @param id		the flight configuration ID
	 * @param value		the parameter value (null not allowed)
	 */
	public void set(FlightConfigurationId fcid, E nextValue) {
		if ( nextValue == null) {
			// null value means to delete this fcid
			this.map.remove(fcid);
		}else{
			this.map.put(fcid, nextValue);
		}

		update();
	}
	
	
	public boolean isDefault(E testVal) {
		 return (Utils.equals( this.getDefault(), testVal));
	}
	
	/**
	 * Return whether a specific flight configuration ID is using the
	 * default value.
	 * 
	 * @param id	the flight configuration ID
	 * @return		whether the default is being used
	 */
	public boolean isDefault( FlightConfigurationId fcid) {
		return ( this.getDefault() == this.map.get(fcid));
	}
	
	/**
	 * Reset a specific flight configuration ID to use the default parameter value.
	 * 
	 * @param id	the flight configuration ID
	 */
	public void reset( FlightConfigurationId fcid) {
		if( fcid.isValid() ){
			set( fcid, null);
		}
	}

	/* 
	 * Clears all configuration-specific settings -- meaning querying the parameter for any configuration will return the default value.
	 */
	public void reset() {
		E tempValue = this.getDefault();
		this.map.clear();
		setDefault(tempValue);
	}
	
	public FlightConfigurationId cloneFlightConfiguration(FlightConfigurationId oldConfigId, FlightConfigurationId newConfigId) {
		// clones the ENTRIES for the given fcid's.
		E oldValue = this.get(oldConfigId);
		this.set(newConfigId, oldValue.clone());
		update();
		return newConfigId;
	}

	
	public String toDebug(){
		StringBuilder buf = new StringBuilder();
		buf.append(String.format("====== Dumping ConfigurationSet<%s> (%d configurations)\n", this.getDefault().getClass().getSimpleName(), this.size() ));
		final String fmt = "    [%-12s]: %s\n";
		for( FlightConfigurationId loopFCID : this.getSortedConfigurationIDs()){
			String shortKey = loopFCID.toShortKey();
			E inst = this.map.get(loopFCID);
			if( this.isDefault(inst)){
				shortKey = "*"+shortKey+"*";
			}
			buf.append(String.format(fmt, shortKey, inst ));
		}
		return buf.toString();
	}


	public void update(){
		for( E curValue: this.map.values() ){
			curValue.update();
		}
	}
			
	
}
