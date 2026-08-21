/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.component.form

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.Gray
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.Red
import com.greenshark.quickattend.ui.theme.Turquoise

/** Outlined text field styled for the app, with inline error support. */
@Composable
fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    supportingText: String? = null,
    errorText: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1,
    imeAction: ImeAction = ImeAction.Next
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        label = { Text(text = label) },
        placeholder = placeholder?.let { { Text(text = it, color = Gray) } },
        supportingText = (errorText ?: supportingText)?.let {
            { Text(text = it, color = if (errorText != null) Red else Gray) }
        },
        isError = errorText != null,
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = imeAction
        ),
        shape = OutlinedTextFieldDefaults.shape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Turquoise,
            focusedLabelColor = Turquoise,
            cursorColor = Turquoise,
            focusedTextColor = Black,
            unfocusedTextColor = Black,
            errorBorderColor = Red,
            errorLabelColor = Red
        )
    )
}

@Preview(showBackground = true)
@Composable
fun FormTextFieldPreview() {
    QuickAttendTheme {
        FormTextField(
            value = "Clase de Yoga",
            onValueChange = {},
            label = "Title",
            supportingText = "Shown to every attendant",
            modifier = Modifier.padding(all = 16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FormTextFieldErrorPreview() {
    QuickAttendTheme {
        FormTextField(
            value = "",
            onValueChange = {},
            label = "Title",
            errorText = "A title is required",
            modifier = Modifier.padding(all = 16.dp)
        )
    }
}
