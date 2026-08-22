/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.component.host

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.greenshark.quickattend.R
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.Gray
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.Turquoise

/** Narrows the attendee list by name or email as the host types. */
@Composable
fun AttendeeSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = {
            Text(text = stringResource(R.string.search_attendants), color = Gray)
        },
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.search),
                contentDescription = null,
                tint = Gray,
                modifier = Modifier.size(size = 20.dp)
            )
        },
        trailingIcon = if (query.isEmpty()) {
            null
        } else {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        painter = painterResource(R.drawable.keyboard_return),
                        contentDescription = stringResource(R.string.action_cancel),
                        tint = Gray,
                        modifier = Modifier.size(size = 18.dp)
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        shape = OutlinedTextFieldDefaults.shape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Turquoise,
            cursorColor = Turquoise,
            focusedTextColor = Black,
            unfocusedTextColor = Black
        )
    )
}

@Preview(showBackground = true, widthDp = 380)
@Composable
fun AttendeeSearchFieldPreview() {
    QuickAttendTheme {
        AttendeeSearchField(
            query = "",
            onQueryChange = {},
            modifier = Modifier.padding(all = 12.dp)
        )
    }
}
