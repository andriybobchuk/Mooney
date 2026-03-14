package com.andriybobchuk.mooney.core.presentation.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.andriybobchuk.mooney.core.presentation.designsystem.MooneyDesignSystem

/**
 * Text field variants
 */
enum class TextFieldVariant {
    OUTLINED,  // Default outlined style
    FILLED     // Filled background style
}

/**
 * Primary text input component for Mooney
 */
@Composable
fun MooneyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    showClearButton: Boolean = false,
    variant: TextFieldVariant = TextFieldVariant.OUTLINED,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    
    Column(modifier = modifier) {
        when (variant) {
            TextFieldVariant.OUTLINED -> {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    readOnly = readOnly,
                    textStyle = MooneyDesignSystem.Typography.BodyLarge,
                    label = label?.let { { Text(it) } },
                    placeholder = placeholder?.let { { 
                        Text(
                            text = it,
                            style = MooneyDesignSystem.Typography.BodyLarge,
                            color = MooneyDesignSystem.Colors.Disabled
                        ) 
                    } },
                    leadingIcon = leadingIcon?.let { {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            tint = if (isFocused) MooneyDesignSystem.Colors.Primary 
                                   else MooneyDesignSystem.Colors.Disabled
                        )
                    } },
                    trailingIcon = {
                        if (showClearButton && value.isNotEmpty() && enabled) {
                            IconButton(
                                onClick = { onValueChange("") }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = MooneyDesignSystem.Colors.Disabled
                                )
                            }
                        } else if (trailingIcon != null) {
                            if (onTrailingIconClick != null) {
                                IconButton(onClick = onTrailingIconClick) {
                                    Icon(
                                        imageVector = trailingIcon,
                                        contentDescription = null
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = trailingIcon,
                                    contentDescription = null
                                )
                            }
                        }
                    },
                    isError = isError || errorText != null,
                    visualTransformation = visualTransformation,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    singleLine = singleLine,
                    maxLines = maxLines,
                    interactionSource = interactionSource,
                    shape = MooneyDesignSystem.Shapes.textField,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MooneyDesignSystem.Colors.Primary,
                        unfocusedBorderColor = MooneyDesignSystem.Colors.Divider,
                        errorBorderColor = MooneyDesignSystem.Colors.Error,
                        focusedLabelColor = MooneyDesignSystem.Colors.Primary,
                        unfocusedLabelColor = MooneyDesignSystem.Colors.Disabled,
                        errorLabelColor = MooneyDesignSystem.Colors.Error
                    )
                )
            }
            
            TextFieldVariant.FILLED -> {
                TextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    readOnly = readOnly,
                    textStyle = MooneyDesignSystem.Typography.BodyLarge,
                    label = label?.let { { Text(it) } },
                    placeholder = placeholder?.let { { 
                        Text(
                            text = it,
                            style = MooneyDesignSystem.Typography.BodyLarge,
                            color = MooneyDesignSystem.Colors.Disabled
                        ) 
                    } },
                    leadingIcon = leadingIcon?.let { {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            tint = if (isFocused) MooneyDesignSystem.Colors.Primary 
                                   else MooneyDesignSystem.Colors.Disabled
                        )
                    } },
                    trailingIcon = {
                        if (showClearButton && value.isNotEmpty() && enabled) {
                            IconButton(
                                onClick = { onValueChange("") }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = MooneyDesignSystem.Colors.Disabled
                                )
                            }
                        } else if (trailingIcon != null) {
                            if (onTrailingIconClick != null) {
                                IconButton(onClick = onTrailingIconClick) {
                                    Icon(
                                        imageVector = trailingIcon,
                                        contentDescription = null
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = trailingIcon,
                                    contentDescription = null
                                )
                            }
                        }
                    },
                    isError = isError || errorText != null,
                    visualTransformation = visualTransformation,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    singleLine = singleLine,
                    maxLines = maxLines,
                    interactionSource = interactionSource,
                    shape = MooneyDesignSystem.Shapes.textField,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MooneyDesignSystem.Colors.Primary,
                        unfocusedBorderColor = MooneyDesignSystem.Colors.Surface,
                        errorBorderColor = MooneyDesignSystem.Colors.Error,
                        focusedLabelColor = MooneyDesignSystem.Colors.Primary,
                        unfocusedLabelColor = MooneyDesignSystem.Colors.Disabled,
                        errorLabelColor = MooneyDesignSystem.Colors.Error
                    )
                )
            }
        }
        
        // Helper or error text
        AnimatedVisibility(
            visible = errorText != null || helperText != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = errorText ?: helperText ?: "",
                style = MooneyDesignSystem.Typography.BodySmall,
                color = if (errorText != null) MooneyDesignSystem.Colors.Error 
                        else MooneyDesignSystem.Colors.Disabled,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

/**
 * Specialized password field with visibility toggle
 */
@Composable
fun MooneyPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Password",
    placeholder: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    variant: TextFieldVariant = TextFieldVariant.OUTLINED,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    var passwordVisible by remember { mutableStateOf(false) }
    
    MooneyTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        helperText = helperText,
        errorText = errorText,
        isError = isError,
        enabled = enabled,
        singleLine = true,
        trailingIcon = Icons.Default.Clear,
        onTrailingIconClick = { passwordVisible = !passwordVisible },
        variant = variant,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        keyboardActions = keyboardActions
    )
}

/**
 * Specialized amount/currency input field
 */
@Composable
fun MooneyAmountField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Amount",
    currencySymbol: String = "$",
    placeholder: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    variant: TextFieldVariant = TextFieldVariant.OUTLINED,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    MooneyTextField(
        value = value,
        onValueChange = { newValue ->
            // Only allow numbers and decimal point
            if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                onValueChange(newValue)
            }
        },
        modifier = modifier,
        label = label,
        placeholder = placeholder ?: "0.00",
        helperText = helperText,
        errorText = errorText,
        isError = isError,
        enabled = enabled,
        singleLine = true,
        leadingIcon = null,
        variant = variant,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done
        ),
        keyboardActions = keyboardActions
    )
}

/**
 * Search field with search icon and clear button
 */
@Composable
fun MooneySearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search...",
    enabled: Boolean = true,
    onSearch: () -> Unit = {}
) {
    MooneyTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = placeholder,
        enabled = enabled,
        singleLine = true,
        leadingIcon = Icons.Default.Clear, // Replace with search icon
        showClearButton = true,
        variant = TextFieldVariant.FILLED,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
            onSearch = { onSearch() }
        )
    )
}