package io.github.linuxforhealth.core.expression.condition;

import com.google.common.base.Preconditions;
import io.github.linuxforhealth.api.Condition;
import io.github.linuxforhealth.api.EvaluationResult;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompoundAndOrCondition implements Condition {
    private final String conditionStatement;

    public CompoundAndOrCondition(String conditionStatement) {
//        Preconditions.checkArgument(conditions != null && !conditions.isEmpty(),
//                "conditions cannot be null or empty");
        this.conditionStatement = conditionStatement;
    }

    @Override
    public boolean test(Map<String, EvaluationResult> contextVariables) {
        String updatedString = conditionStatement.toLowerCase();
        // order is important here for the null replacements
        updatedString = updatedString.replaceAll("\\snull", "== null");
        updatedString = updatedString.replaceAll("not_null", "!= null");
        updatedString = updatedString.replaceAll("equals", "==");
        updatedString = updatedString.replaceAll("not_equals", "!=");
        for(String currentContextVariables: contextVariables.keySet()) {
            String currentValue = contextVariables.get(currentContextVariables).getValue();
            if(!currentValue.equals("null")) {
                updatedString = updatedString.replaceAll("\\$" + currentContextVariables, currentValue);
            } else {
                updatedString = updatedString.replaceAll("\\$" + currentContextVariables, currentValue);
            }

        }

        Pattern pattern = Pattern.compile("\\b[^null\\s]\\w+");
        Matcher matcher = pattern.matcher(updatedString);
        updatedString = matcher.replaceAll(matchResult -> "'" + matchResult.group() + "'");

        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("JavaScript");
        boolean result;
        try {
            result = (Boolean) se.eval(updatedString);
        } catch (ScriptException e) {
            throw new RuntimeException("Could not evaluate condition: " + conditionStatement);
        }

        return result;
    }
}
