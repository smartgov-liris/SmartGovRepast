package policyagent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import environment.Structure;

/**
 * Stores information about structures and their names
 * @author Simon
 *
 */
public class Perimeter {

	private List<String> structuresName;
	private List<Structure> structures;
	private Map<String, List<Structure>> structuresByName;
	
	public Perimeter() {
		this.structures = new ArrayList<>();
		this.structuresName = new ArrayList<>();
		this.structuresByName = new HashMap<>();
	}
	
	public Perimeter(Perimeter perimeter) {
		this.structures = new ArrayList<>();
		for(int i = 0; i < perimeter.getStructures().size(); i++){
			structures.add(perimeter.getStructures().get(i));
		}
		this.structuresName = new ArrayList<>();
		updateStructuresName(structures);
		this.structuresByName = new HashMap<>();
		this.structuresByName = initStructuresByName(structuresName, structures);
	}
	
	public Perimeter(List<Structure> structures) {
		this.structures = structures;
		this.structuresName = new ArrayList<>();
		updateStructuresName(structures);
		this.structuresByName = new HashMap<>();
		this.structuresByName = initStructuresByName(structuresName, structures);
	}
	
	public Perimeter(Map<String, List<Structure>> structuresByName) {
		this();
		this.structuresByName = structuresByName;
		deconstructStructuresByName(structuresByName);
	}
	
	private void updateStructuresName(List<Structure> structures) {
		for(int i = 0; i < structures.size(); i++) {
			if(!structuresName.contains(structures.get(i).getClassName())) {
				structuresName.add(structures.get(i).getClassName());
			}
		}
	}
	
	private Map<String, List<Structure>> initStructuresByName(List<String> structuresName, List<Structure> structures){
		Map<String, List<Structure>> structuresSortByName = new HashMap<>();
		for(int indexOfName = 0; indexOfName < structuresName.size(); indexOfName++) {
			List<Structure> structuresWithSpecificName = new ArrayList<>();
			for(int indexOfStructure = 0; indexOfStructure < structures.size(); indexOfStructure++) {
				if(structuresName.get(indexOfName).equals(structures.get(indexOfStructure).getClassName())) {
					structuresWithSpecificName.add(structures.get(indexOfStructure));
				}
			}
			structuresSortByName.put(structuresName.get(indexOfName), structuresWithSpecificName);
		}
		return structuresSortByName;
	}
	
	private void updateStructuresByName(Map<String, List<Structure>> newStructuresByName) {
		for(Entry<String, List<Structure>> entry : newStructuresByName.entrySet()) {
			if(!structuresByName.containsKey(entry.getKey())){
				structuresByName.put(entry.getKey(), entry.getValue());
			} else {
				for(Structure structure : entry.getValue()) {
					structuresByName.get(entry.getKey()).add(structure);
				}
			}
		}
	}
	
	private void deconstructStructuresByName(Map<String, List<Structure>> structuresByName) {
		for(Entry<String, List<Structure>> entry : structuresByName.entrySet()) {
			String key = entry.getKey();
			if(!structuresName.contains(key)) {
				structuresName.add(key);
			}
			List<Structure> structures = entry.getValue();
			for(int indexOfStructure = 0; indexOfStructure < structures.size(); indexOfStructure++) {
				this.structures.add(structures.get(indexOfStructure));
			}
		}
	}
	
	public List<Structure> getStructuresWithName(String name){
		if(structuresByName.get(name) == null) {
			return new ArrayList<>();
		}
		return structuresByName.get(name);
	}
	
	public List<Structure> getStructures() {
		return structures;
	}
	
	public void removeStructures(List<Structure> structures) {
		for(Structure structure : structures) {
			if(getStructuresWithName(structure.getClassName()).isEmpty()) {
				structuresName.remove(structure.getClassName());
				structuresByName.remove(structure.getClassName());
			} else {
				List<Structure> structuresToUpdate = structuresByName.get(structure.getClassName());
				structuresToUpdate.remove(structure);
				if(structuresToUpdate.size() == 0) {
					structuresName.remove(structure.getClassName());
					structuresByName.remove(structure.getClassName());
				} else {
					structuresByName.put(structure.getClassName(), structuresToUpdate);
				}
			}
		}

		
		this.structures.removeAll(structures);
	}
	
	public void mergePerimeters(Perimeter perimeter) {
		for(int i = 0; i < perimeter.getStructures().size(); i++) {
			structures.add(perimeter.getStructures().get(i));
		}
		updateStructuresName(perimeter.getStructures());
		updateStructuresByName(perimeter.getStructuresByName());
		perimeter.removeStructures(perimeter.getStructures());
	}
	
	public Map<String, List<Structure>> getStructuresByName() {
		return structuresByName;
	}
	
	@Override
	public String toString() {
		String str = "Number of structures: " + structures.size() + "\n[";
		if(structures.size() > 1) {
			for(int i = 0; i < structures.size() - 1; i++) {
				str += structures.get(i).getID() + ",";
			}
		}
		if(structures.size() == 0) {
			str += "]\n";
		} else {
			str += structures.get(structures.size() - 1).getID() + "]\n";
		}
		str += "Number of structures names: " + structuresName.size() + "\n[";
		if(structuresName.size() > 1) {
			for(int i = 0; i < structuresName.size() - 1; i++) {
				str += structuresName.get(i) + ",";
			}
		}if(structuresName.size() == 0) {
			str += "]\n";
		} else {
			str += structuresName.get(structuresName.size() - 1) + "]\n";
		}
		str += "Structures per name: " + structuresByName.size() + "\n{";
		if(structuresByName.size() > 0) {
			for(Entry<String, List<Structure>> entry : structuresByName.entrySet()) {
				str += entry.getKey() + ": ";
				for(Structure structure : entry.getValue()) {
					str += structure.getID() + ",";
				}
			}
		}
		str += "}\n";
		return str;
	}
	
}
