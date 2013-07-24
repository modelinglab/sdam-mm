/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.modelinglab.mm.sdam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import org.modelinglab.ocl.core.ast.Element;
import org.modelinglab.ocl.core.ast.expressions.BooleanLiteralExp;
import org.modelinglab.ocl.core.ast.expressions.OclExpression;
import org.modelinglab.ocl.core.ast.expressions.OperationCallExp;
import org.modelinglab.ocl.core.standard.operations.bool.Or;

/**
 *
 * @author Miguel Angel Garcia de Dios <miguelangel.garcia at imdea.org>
 */
public class SdamFactory {
    private final Map<String, Map<Element, Map<AccessEnum, OclExpression>>> permissionTable = new HashMap<>();
    
     public void addPermissionConstraint(String roleId, Element resource, AccessEnum accessType, OclExpression constraint) {
        Map<Element, Map<AccessEnum, OclExpression>> byRole = permissionTable.get(roleId);
        if (byRole == null) {
            byRole = new HashMap<>();
            permissionTable.put(roleId, byRole);
        }
        Map<AccessEnum, OclExpression> byResource = byRole.get(resource);
        if (byResource == null) {
            byResource = new EnumMap<>(AccessEnum.class);
            byRole.put(resource, byResource);
        }
        OclExpression oclExpression = byResource.get(accessType);
        if (oclExpression == null) {
            byResource.put(accessType, constraint);
        }
        else{
            OclExpression newOclExpression = makeDisjunction(oclExpression, constraint);
            byResource.put(accessType, newOclExpression);
        }
    }

    private OclExpression makeDisjunction(OclExpression oclExpression1, OclExpression oclExpression2) {
        OclExpression result;
        
        if(oclExpression1 instanceof BooleanLiteralExp){
            BooleanLiteralExp booleanExp1 = (BooleanLiteralExp) oclExpression1;
            if(booleanExp1.getValue()){
                result = oclExpression1;
            }
            else{
                result = oclExpression2;
            }
        }
        else if(oclExpression2 instanceof BooleanLiteralExp){
            BooleanLiteralExp booleanExp2 = (BooleanLiteralExp) oclExpression2;
            if(booleanExp2.getValue()){
                result = oclExpression2;
            }
            else{
                result = oclExpression1;
            }
        }
        else{
            OperationCallExp disjunction = new OperationCallExp();
            disjunction.setReferredOperation(Or.getInstance());
            disjunction.setSource(oclExpression1);
            Collection<OclExpression> arguments = new ArrayList<>();
            arguments.add(oclExpression2);
            disjunction.setArguments(arguments);
            result = disjunction;
        }
        return result;
    }

    public Map<String, Map<Element, Map<AccessEnum, OclExpression>>> getPermissionTable() {
        return permissionTable;
    }
    
    public Sdam createSdam(String sdamName) {
        return new Sdam(sdamName, permissionTable);
    }
}
