/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.modelinglab.mm.sdam;

import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.modelinglab.ocl.core.ast.Element;
import org.modelinglab.ocl.core.ast.Property;
import org.modelinglab.ocl.core.ast.UmlClass;
import org.modelinglab.ocl.core.ast.expressions.BooleanLiteralExp;
import org.modelinglab.ocl.core.ast.expressions.OclExpression;
import org.modelinglab.ocl.core.ast.types.PrimitiveType;

/**
 *
 * @author Gonzalo Ortiz Jaureguizar (gortiz at software.imdea.org)
 */
public class Sdam implements Serializable {

	private static final long serialVersionUID = 1L;
	
    private final String modelName;
    
    private final Map<String, Map<Element, EnumMap<AccessEnum, OclExpression>>> permissionTable;

    public Sdam(String modelName, Map<String, Map<Element, Map<AccessEnum, OclExpression>>> permissionTable) {
        this.modelName = modelName;

        PrimitiveType booleanOclType = PrimitiveType.getInstance(PrimitiveType.PrimitiveKind.BOOLEAN);
        
        Map<String, Map<Element, EnumMap<AccessEnum, OclExpression>>> byRole;
        byRole = new HashMap<>(permissionTable.size());
        
        /*
         * We need to clone the table
         */
        for (String role : permissionTable.keySet()) {
            Map<Element, Map<AccessEnum, OclExpression>> byResource = permissionTable.get(role);
            Map<Element, EnumMap<AccessEnum, OclExpression>> byResourceCopy = new HashMap<>(byResource.size());
            
            for (Element element : byResource.keySet()) {
                Map<AccessEnum, OclExpression> byAccess = byResource.get(element);
                EnumMap<AccessEnum, OclExpression> byAccessCopy = new EnumMap<>(byAccess);
//TODO: Constraints should be inmutables --> fix it in OCL CORE in order to make it works
//                for (OclExpression constraint : byAccessCopy.values()) {
//                    Preconditions.checkArgument(constraint.isImmutable());
//                }
                for (final OclExpression oclExpression : byAccessCopy.values()) {
                    Preconditions.checkArgument(oclExpression.getType().equals(booleanOclType), 
                            oclExpression + " is not valid as constraint because its type is not boolean!");
                }
                
                byResourceCopy.put(element, byAccessCopy);
            }
            
            byRole.put(role, byResourceCopy);
        }
        this.permissionTable = byRole;
    }

    public String getModelName() {
        return modelName;
    }
    
    /**
     * Returns all declared roles.
     * 
     * It is not guaranteed that every element (role) of the returned set has permission to do something,
     * but is guaranteed that all roles that has permission to do something are returned.
     * 
     * @return all declared roles of 
     */
    public Set<String> getAllRoles() {
        return permissionTable.keySet();
    }
    
    public OclExpression getPermissionConstraint(String roleId, UmlClass resource, AccessEnum accessType) {
        assert accessType == AccessEnum.CREATE || accessType == AccessEnum.DELETE
                : "UmlClasses can only be accessed to create or delete!";
        
        return getPermissionConstraint(roleId, (Element) resource, accessType);
    }
    
    public OclExpression getPermissionConstraint(String roleId, Property resource, AccessEnum accessType) {
        assert (accessType != AccessEnum.CREATE && accessType != AccessEnum.DELETE) || resource.isMultivalued()
                : "Non multievaluated properties cannot be accessed to create or delete!";
        
        return getPermissionConstraint(roleId, (Element) resource, accessType);
    }
    
    private OclExpression getPermissionConstraint(String roleId, Element resource, AccessEnum accessType) {
        Map<Element, EnumMap<AccessEnum, OclExpression>> byRole = permissionTable.get(roleId);
        if (byRole == null) {
            return new BooleanLiteralExp(Boolean.FALSE);
        }
        Map<AccessEnum, OclExpression> byResource = byRole.get(resource);
        if (byResource == null) {
            return new BooleanLiteralExp(Boolean.FALSE);
        }
        OclExpression result = byResource.get(accessType);
        if (result == null) {
            return new BooleanLiteralExp(Boolean.FALSE);
        }
        return result;
    }

	@Override
	public String toString() {
		return modelName + " security model";
	}

    public Map<String, Map<Element, EnumMap<AccessEnum, OclExpression>>> getPermissionTable() {
        return permissionTable;
    }
}
