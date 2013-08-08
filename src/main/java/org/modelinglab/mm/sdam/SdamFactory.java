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
import org.modelinglab.ocl.core.ast.Property;
import org.modelinglab.ocl.core.ast.expressions.BooleanLiteralExp;
import org.modelinglab.ocl.core.ast.expressions.OclExpression;
import org.modelinglab.ocl.core.ast.expressions.OperationCallExp;
import org.modelinglab.ocl.core.ast.types.PrimitiveType;
import org.modelinglab.ocl.core.ast.types.PrimitiveType.PrimitiveKind;
import org.modelinglab.ocl.core.standard.operations.bool.Or;

import static org.modelinglab.mm.sdam.AccessEnum.DELETE;

/**
 *
 * @author Miguel Angel Garcia de Dios <miguelangel.garcia at imdea.org>
 */
public class SdamFactory {

    private final Map<String, Map<Element, Map<AccessEnum, OclExpression>>> permissionTable = new HashMap<>();

    public void addPermissionConstraint(String roleId, Element resource, AccessEnum accessType,
                                        OclExpression constraint) {
        
        checkValidPermission(resource, accessType, constraint);
        
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
        else {
            OclExpression newOclExpression = makeDisjunction(oclExpression, constraint);
            byResource.put(accessType, newOclExpression);
        }
    }

    private void checkValidPermission(Element resource, AccessEnum accessType, OclExpression constraint) {
        if (constraint == null) {
            throw new IllegalArgumentException("The constraint must be not null");
        }
        if (constraint.getType() == null) {
            throw new IllegalArgumentException("The constraint has no type");
        }
        if (!constraint.getType().equals(PrimitiveType.getInstance(PrimitiveKind.BOOLEAN))) {
            throw new IllegalArgumentException("The constraint is not a boolean expression");
        }
        switch (accessType) {
            default:
                throw new IllegalArgumentException(accessType + " is not a valid access type");
            case CREATE:
            case DELETE:
                if (resource instanceof Property && !((Property) resource).isMultivalued()) {
                    throw new IllegalArgumentException("A scalar property (as " + accessType + ") cannot be "
                                                       + "associated with a " + accessType + " action");
                }
                break;
            case READ:
            case UPDATE:
                if (!(resource instanceof Property)) {
                    throw new IllegalArgumentException(accessType + " actions can only be associated with "
                                                       + "properties and " + resource + " is not a property");
                }
                break;
        }

    }

    private OclExpression makeDisjunction(OclExpression oclExpression1, OclExpression oclExpression2) {
        OclExpression result;

        if (oclExpression1 instanceof BooleanLiteralExp) {
            BooleanLiteralExp booleanExp1 = (BooleanLiteralExp) oclExpression1;
            if (booleanExp1.getValue()) {
                result = oclExpression1;
            }
            else {
                result = oclExpression2;
            }
        }
        else if (oclExpression2 instanceof BooleanLiteralExp) {
            BooleanLiteralExp booleanExp2 = (BooleanLiteralExp) oclExpression2;
            if (booleanExp2.getValue()) {
                result = oclExpression2;
            }
            else {
                result = oclExpression1;
            }
        }
        else {
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
